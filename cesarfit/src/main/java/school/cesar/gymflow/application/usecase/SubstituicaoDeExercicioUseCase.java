package school.cesar.gymflow.application.usecase;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.atendimento.model.MotivoDeSubstituicao;
import school.cesar.gymflow.domain.atendimento.model.SolicitacaoDeSubstituicao;
import school.cesar.gymflow.domain.atendimento.repositorio.RepositorioDeSolicitacoes;
import school.cesar.gymflow.domain.atendimento.repositorio.RepositorioDeEquipamentosIndisponiveis;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;
import school.cesar.gymflow.domain.treino.model.Exercicio;
import school.cesar.gymflow.domain.treino.model.FichaDeTreino;
import school.cesar.gymflow.domain.treino.model.ItemDeTreino;
import school.cesar.gymflow.domain.treino.repositorio.RepositorioDeExercicios;
import school.cesar.gymflow.domain.treino.repositorio.RepositorioDeFichas;

/**
 * Funcionalidade: solicitacao de substituicao de exercicio.
 * - limites por ficha: 3 trocas aprovadas no total e 1 por PREFERENCIA;
 * - o sistema sugere substitutos (mesmo grupo, ativos, fora da ficha, equipamento nao interditado);
 * - EQUIPAMENTO_INDISPONIVEL com o equipamento interditado e aprovado automaticamente com a melhor sugestao.
 *   O contexto Atendimento sabe quais equipamentos estao interditados pela sua propria visao
 *   (RepositorioDeEquipamentosIndisponiveis), alimentada pelos eventos do contexto Operacao;
 * - fila da equipe ordenada por prioridade (dor/lesao primeiro) e antiguidade; pedidos expiram em 7 dias.
 */
@Service
public class SubstituicaoDeExercicioUseCase {

    public static final int MAX_TROCAS_POR_FICHA = 3;
    public static final int MAX_TROCAS_POR_PREFERENCIA = 1;

    private final RepositorioDeSolicitacoes solicitacoes;
    private final RepositorioDeFichas fichas;
    private final RepositorioDeExercicios exercicios;
    private final RepositorioDeEquipamentosIndisponiveis indisponiveis;
    private final NomesDeAlunos nomes;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;

    public SubstituicaoDeExercicioUseCase(RepositorioDeSolicitacoes solicitacoes, RepositorioDeFichas fichas,
                                          RepositorioDeExercicios exercicios,
                                          RepositorioDeEquipamentosIndisponiveis indisponiveis,
                                          NomesDeAlunos nomes, PublicadorDeEventos publicador, Relogio relogio) {
        this.solicitacoes = solicitacoes;
        this.fichas = fichas;
        this.exercicios = exercicios;
        this.indisponiveis = indisponiveis;
        this.nomes = nomes;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public Respostas.SubstituicaoResumo solicitar(Long alunoId, Comandos.SolicitarSubstituicao comando) {
        LocalDateTime agora = relogio.agora();
        FichaDeTreino ficha = fichas.porId(comando.fichaId())
                .filter(f -> f.alunoId().equals(alunoId))
                .orElseThrow(() -> new RegraDeNegocioException("Ficha nao pertence ao aluno"));
        ItemDeTreino item = ficha.item(comando.posicao());
        if (item.concluido()) {
            throw new RegraDeNegocioException("Exercicio ja concluido nao pode ser substituido");
        }
        MotivoDeSubstituicao categoria = MotivoDeSubstituicao.de(comando.categoria());
        List<SolicitacaoDeSubstituicao> daFicha = solicitacoes.substituicoesDoAluno(alunoId).stream()
                .filter(s -> s.fichaId().equals(ficha.id())).toList();
        if (daFicha.stream().anyMatch(s -> s.pendente(agora) && s.posicao() == comando.posicao())) {
            throw new RegraDeNegocioException("Ja existe um pedido de substituicao pendente para este exercicio");
        }
        if (daFicha.stream().filter(SolicitacaoDeSubstituicao::aprovada).count() >= MAX_TROCAS_POR_FICHA) {
            throw new RegraDeNegocioException("Limite de " + MAX_TROCAS_POR_FICHA
                    + " substituicoes aprovadas por ficha atingido: peca uma nova ficha ao instrutor");
        }
        if (categoria == MotivoDeSubstituicao.PREFERENCIA && daFicha.stream()
                .filter(s -> s.aprovada() && s.categoria() == MotivoDeSubstituicao.PREFERENCIA).count()
                >= MAX_TROCAS_POR_PREFERENCIA) {
            throw new RegraDeNegocioException("Troca por preferencia permitida apenas uma vez por ficha");
        }
        SolicitacaoDeSubstituicao salva = solicitacoes.salvar(SolicitacaoDeSubstituicao.solicitar(
                alunoId, ficha.id(), comando.posicao(), item.exercicio(), comando.motivo(), categoria, agora));
        publicar(salva, "solicitada [" + categoria + "]: " + salva.motivo());

        if (categoria == MotivoDeSubstituicao.EQUIPAMENTO_INDISPONIVEL) {
            String equipamento = exercicios.porNome(item.exercicio()).map(Exercicio::equipamento).orElse(null);
            List<Exercicio> sugestoes = sugestoes(ficha, comando.posicao());
            if (interditado(equipamento) && !sugestoes.isEmpty()) {
                return efetivar(salva, ficha, sugestoes.get(0),
                        "Aprovada automaticamente: " + equipamento + " interditado por ocorrencia", true);
            }
        }
        return resumo(salva, nomes.de(alunoId), agora);
    }

    @Transactional
    public Respostas.SubstituicaoResumo aprovar(Long id, Comandos.AprovarSubstituicao comando) {
        LocalDateTime agora = relogio.agora();
        SolicitacaoDeSubstituicao s = buscar(id);
        if (!s.pendente(agora)) {
            throw new RegraDeNegocioException(s.expirada(agora)
                    ? "Solicitacao expirada: o aluno deve fazer um novo pedido" : "Solicitacao ja foi respondida");
        }
        if (comando.exercicioId() == null) {
            throw new RegraDeNegocioException("Escolha o exercicio substituto");
        }
        Exercicio novo = exercicios.porId(comando.exercicioId())
                .orElseThrow(() -> new RegraDeNegocioException("Exercicio substituto nao encontrado"));
        FichaDeTreino ficha = fichas.porId(s.fichaId())
                .orElseThrow(() -> new RegraDeNegocioException("Ficha nao encontrada"));
        if (!ficha.item(s.posicao()).exercicio().equalsIgnoreCase(s.exercicioAtual())) {
            throw new RegraDeNegocioException("O exercicio da ficha mudou desde o pedido; rejeite e peca um novo");
        }
        ItemDeTreino item = ficha.item(s.posicao());
        if (novo.grupoMuscular() != item.grupoMuscular()) {
            throw new RegraDeNegocioException("O substituto deve trabalhar o mesmo grupo muscular ("
                    + item.grupoMuscular() + ")");
        }
        if (naFicha(ficha, novo.nome())) {
            throw new RegraDeNegocioException("O exercicio " + novo.nome() + " ja esta na ficha do aluno");
        }
        if (interditado(novo.equipamento())) {
            throw new RegraDeNegocioException("O equipamento " + novo.equipamento() + " esta interditado por ocorrencia");
        }
        return efetivar(s, ficha, novo, comando.observacao(), false);
    }

    @Transactional
    public Respostas.SubstituicaoResumo rejeitar(Long id, String justificativa) {
        SolicitacaoDeSubstituicao s = buscar(id);
        s.rejeitar(justificativa, relogio.agora());
        SolicitacaoDeSubstituicao salva = solicitacoes.salvar(s);
        publicar(salva, "rejeitada: " + salva.resposta());
        return resumo(salva, nomes.de(salva.alunoId()), relogio.agora());
    }

    @Transactional(readOnly = true)
    public List<Respostas.SubstituicaoResumo> doAluno(Long alunoId) {
        String nome = nomes.de(alunoId);
        LocalDateTime agora = relogio.agora();
        return solicitacoes.substituicoesDoAluno(alunoId).stream().map(s -> resumo(s, nome, agora)).toList();
    }

    /** Fila da equipe: pendentes primeiro, por prioridade da categoria e depois pelo pedido mais antigo. */
    @Transactional(readOnly = true)
    public List<Respostas.SubstituicaoResumo> todas() {
        Map<Long, String> mapa = nomes.mapa();
        LocalDateTime agora = relogio.agora();
        return solicitacoes.substituicoes().stream()
                .sorted(Comparator.comparing((SolicitacaoDeSubstituicao s) -> !s.pendente(agora))
                        .thenComparing(s -> -s.categoria().prioridade())
                        .thenComparing(SolicitacaoDeSubstituicao::criadaEm))
                .map(s -> resumo(s, mapa.getOrDefault(s.alunoId(), "-"), agora)).toList();
    }

    private Respostas.SubstituicaoResumo efetivar(SolicitacaoDeSubstituicao s, FichaDeTreino ficha, Exercicio novo,
                                                  String observacao, boolean automatica) {
        ficha.substituirExercicio(s.posicao(), novo);
        fichas.salvar(ficha);
        s.aprovar(novo.nome(), observacao, automatica, relogio.agora());
        SolicitacaoDeSubstituicao salva = solicitacoes.salvar(s);
        publicar(salva, (automatica ? "aprovada automaticamente: " : "aprovada: ")
                + salva.exercicioAtual() + " -> " + salva.exercicioNovo());
        return resumo(salva, nomes.de(salva.alunoId()), relogio.agora());
    }

    /**
     * Substitutos possiveis, do melhor para o pior: mesmo grupo muscular, ativos, que ainda nao estao na ficha,
     * cujo equipamento nao esta interditado; primeiro os que usam OUTRO equipamento (ou peso corporal).
     */
    List<Exercicio> sugestoes(FichaDeTreino ficha, int posicao) {
        ItemDeTreino item = ficha.item(posicao);
        String equipamentoAtual = exercicios.porNome(item.exercicio()).map(Exercicio::equipamento).orElse(null);
        return exercicios.ativos().stream()
                .filter(e -> e.grupoMuscular() == item.grupoMuscular())
                .filter(e -> !naFicha(ficha, e.nome()))
                .filter(e -> !interditado(e.equipamento()))
                .sorted(Comparator.comparing((Exercicio e) -> Objects.equals(e.equipamento(), equipamentoAtual))
                        .thenComparing(Exercicio::nome))
                .toList();
    }

    private static boolean naFicha(FichaDeTreino ficha, String nome) {
        return ficha.itens().stream().anyMatch(i -> i.exercicio().equalsIgnoreCase(nome));
    }

    private boolean interditado(String equipamento) {
        return equipamento != null && indisponiveis.indisponivel(equipamento);
    }

    private SolicitacaoDeSubstituicao buscar(Long id) {
        return solicitacoes.substituicaoPorId(id)
                .orElseThrow(() -> new RegraDeNegocioException("Solicitacao de substituicao nao encontrada"));
    }

    private void publicar(SolicitacaoDeSubstituicao s, String detalhe) {
        publicador.publicar(EventoDeDominio.de(EventoDeDominio.SUBSTITUICAO_EXERCICIO, String.valueOf(s.id()),
                "Substituicao de " + s.exercicioAtual() + " " + detalhe));
    }

    private Respostas.SubstituicaoResumo resumo(SolicitacaoDeSubstituicao s, String nome, LocalDateTime agora) {
        Optional<FichaDeTreino> ficha = fichas.porId(s.fichaId()).filter(f -> s.posicao() < f.itens().size());
        String grupo = ficha.map(f -> f.itens().get(s.posicao()).grupoMuscular().name()).orElse(null);
        List<String> sugeridos = s.pendente(agora) && ficha.isPresent()
                ? sugestoes(ficha.get(), s.posicao()).stream().limit(3).map(Exercicio::nome).toList()
                : List.of();
        return new Respostas.SubstituicaoResumo(s.id(), s.alunoId(), nome, s.fichaId(), s.posicao(),
                s.exercicioAtual(), grupo, s.motivo(), s.statusEm(agora).name(), s.exercicioNovo(), s.resposta(),
                s.criadaEm(), s.categoria().name(), s.categoria().prioridade(), s.automatica(), sugeridos);
    }
}
