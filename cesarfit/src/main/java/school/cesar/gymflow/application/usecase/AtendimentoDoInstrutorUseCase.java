package school.cesar.gymflow.application.usecase;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.atendimento.model.SolicitacaoDeAtendimento;
import school.cesar.gymflow.domain.atendimento.model.TipoDeAtendimento;
import school.cesar.gymflow.domain.atendimento.repositorio.RepositorioDeSolicitacoes;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;
import school.cesar.gymflow.domain.treino.model.FichaDeTreino;
import school.cesar.gymflow.domain.treino.repositorio.RepositorioDeFichas;

/**
 * Funcionalidade: atendimento do instrutor com fila priorizada.
 * - um pedido em aberto por aluno; dor/lesao entra como prioridade ALTA;
 * - fila: ALTA antes de NORMAL e, dentro da mesma prioridade, o mais antigo primeiro;
 * - nao e permitido assumir um NORMAL enquanto houver ALTA aguardando;
 * - cada instrutor atende no maximo 3 alunos ao mesmo tempo (vale tambem para transferencias).
 */
@Service
public class AtendimentoDoInstrutorUseCase {

    public static final int MAX_SIMULTANEOS_POR_INSTRUTOR = 3;

    private static final Comparator<SolicitacaoDeAtendimento> ORDEM_DA_FILA =
            Comparator.comparing((SolicitacaoDeAtendimento a) -> !a.tipo().prioritario())
                    .thenComparing(SolicitacaoDeAtendimento::abertaEm);

    private final RepositorioDeSolicitacoes solicitacoes;
    private final RepositorioDeFichas fichas;
    private final NomesDeAlunos nomes;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;

    public AtendimentoDoInstrutorUseCase(RepositorioDeSolicitacoes solicitacoes, RepositorioDeFichas fichas,
                                         NomesDeAlunos nomes, PublicadorDeEventos publicador, Relogio relogio) {
        this.solicitacoes = solicitacoes;
        this.fichas = fichas;
        this.nomes = nomes;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public Respostas.AtendimentoResumo abrir(Long alunoId, Comandos.AbrirAtendimento comando) {
        FichaDeTreino ficha = fichas.porAluno(alunoId).stream().findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("Voce precisa ter uma ficha de treino para pedir atendimento"));
        if (solicitacoes.atendimentosDoAluno(alunoId).stream().anyMatch(SolicitacaoDeAtendimento::emAberto)) {
            throw new RegraDeNegocioException("Voce ja tem um pedido de atendimento em aberto");
        }
        String exercicio = comando.posicao() == null ? null : ficha.item(comando.posicao()).exercicio();
        TipoDeAtendimento tipo = TipoDeAtendimento.de(comando.tipo());
        return salvar(SolicitacaoDeAtendimento.abrir(alunoId, ficha.id(), exercicio, comando.motivo(), tipo, false,
                relogio.agora()), "aberto (" + tipo.prioridade() + ")");
    }

    /**
     * Aberto pelo sistema (ex.: feedback pos-treino com relato de dor). Nao abre se o aluno ja tem
     * atendimento em aberto ou ainda nao tem ficha.
     */
    @Transactional
    public Optional<Respostas.AtendimentoResumo> abrirAutomatico(Long alunoId, String motivo) {
        Optional<FichaDeTreino> ficha = fichas.porAluno(alunoId).stream().findFirst();
        if (ficha.isEmpty() || solicitacoes.atendimentosDoAluno(alunoId).stream().anyMatch(SolicitacaoDeAtendimento::emAberto)) {
            return Optional.empty();
        }
        return Optional.of(salvar(SolicitacaoDeAtendimento.abrir(alunoId, ficha.get().id(), null, motivo,
                TipoDeAtendimento.DOR_OU_LESAO, true, relogio.agora()), "aberto automaticamente (ALTA)"));
    }

    @Transactional
    public Respostas.AtendimentoResumo assumir(Long id, String instrutor) {
        SolicitacaoDeAtendimento s = buscar(id);
        List<SolicitacaoDeAtendimento> todos = solicitacoes.atendimentos();
        if (s.aguardando() && !s.tipo().prioritario()
                && todos.stream().anyMatch(a -> a.aguardando() && a.tipo().prioritario())) {
            throw new RegraDeNegocioException("Ha atendimento prioritario (dor ou lesao) aguardando na fila");
        }
        exigirCapacidade(todos, instrutor);
        s.assumir(instrutor, relogio.agora());
        return salvar(s, "assumido por " + s.instrutor() + " apos " + s.minutosDeEspera(relogio.agora()) + " min");
    }

    @Transactional
    public Respostas.AtendimentoResumo transferir(Long id, String novoInstrutor) {
        SolicitacaoDeAtendimento s = buscar(id);
        String anterior = s.instrutor();
        exigirCapacidade(solicitacoes.atendimentos(), novoInstrutor);
        s.transferir(novoInstrutor);
        return salvar(s, "transferido de " + anterior + " para " + s.instrutor());
    }

    @Transactional
    public Respostas.AtendimentoResumo concluir(Long id, String orientacao) {
        SolicitacaoDeAtendimento s = buscar(id);
        s.concluir(orientacao, relogio.agora());
        return salvar(s, "concluido: " + s.orientacao());
    }

    @Transactional
    public Respostas.AtendimentoResumo cancelar(Long alunoId, Long id) {
        SolicitacaoDeAtendimento s = doAluno(alunoId, id);
        s.cancelar();
        return salvar(s, "cancelado pelo aluno");
    }

    @Transactional
    public Respostas.AtendimentoResumo avaliar(Long alunoId, Long id, Comandos.AvaliarAtendimento comando) {
        SolicitacaoDeAtendimento s = doAluno(alunoId, id);
        if (comando.nota() == null) {
            throw new RegraDeNegocioException("Informe a nota de 1 a 5");
        }
        s.avaliar(comando.nota(), comando.comentario(), relogio.agora());
        return salvar(s, "avaliado com nota " + s.nota() + "/5");
    }

    @Transactional(readOnly = true)
    public List<Respostas.AtendimentoResumo> doAluno(Long alunoId) {
        String nome = nomes.de(alunoId);
        List<SolicitacaoDeAtendimento> fila = fila();
        return solicitacoes.atendimentosDoAluno(alunoId).stream().map(a -> resumo(a, nome, fila)).toList();
    }

    /** Lista da equipe: primeiro a fila de espera (na ordem de atendimento), depois os demais. */
    @Transactional(readOnly = true)
    public List<Respostas.AtendimentoResumo> todos() {
        Map<Long, String> mapa = nomes.mapa();
        List<SolicitacaoDeAtendimento> fila = fila();
        return solicitacoes.atendimentos().stream()
                .sorted(Comparator.comparing((SolicitacaoDeAtendimento a) -> !a.aguardando())
                        .thenComparing((a, b) -> a.aguardando() ? ORDEM_DA_FILA.compare(a, b)
                                : b.abertaEm().compareTo(a.abertaEm())))
                .map(a -> resumo(a, mapa.getOrDefault(a.alunoId(), "-"), fila)).toList();
    }

    private List<SolicitacaoDeAtendimento> fila() {
        return solicitacoes.atendimentos().stream().filter(SolicitacaoDeAtendimento::aguardando)
                .sorted(ORDEM_DA_FILA).toList();
    }

    private void exigirCapacidade(List<SolicitacaoDeAtendimento> todos, String instrutor) {
        long emAndamento = todos.stream().filter(a -> a.emAndamentoCom(instrutor)).count();
        if (emAndamento >= MAX_SIMULTANEOS_POR_INSTRUTOR) {
            throw new RegraDeNegocioException("O instrutor " + instrutor + " ja tem "
                    + MAX_SIMULTANEOS_POR_INSTRUTOR + " atendimentos em andamento");
        }
    }

    private SolicitacaoDeAtendimento buscar(Long id) {
        return solicitacoes.atendimentoPorId(id)
                .orElseThrow(() -> new RegraDeNegocioException("Pedido de atendimento nao encontrado"));
    }

    private SolicitacaoDeAtendimento doAluno(Long alunoId, Long id) {
        SolicitacaoDeAtendimento s = buscar(id);
        if (!s.alunoId().equals(alunoId)) {
            throw new RegraDeNegocioException("Pedido de atendimento nao encontrado");
        }
        return s;
    }

    private Respostas.AtendimentoResumo salvar(SolicitacaoDeAtendimento s, String detalhe) {
        SolicitacaoDeAtendimento salva = solicitacoes.salvar(s);
        String nome = nomes.de(salva.alunoId());
        publicador.publicar(EventoDeDominio.de(EventoDeDominio.ATENDIMENTO_INSTRUTOR, String.valueOf(salva.id()),
                "Atendimento de " + nome + " " + detalhe));
        return resumo(salva, nome, fila());
    }

    private Respostas.AtendimentoResumo resumo(SolicitacaoDeAtendimento a, String nome,
                                               List<SolicitacaoDeAtendimento> fila) {
        LocalDateTime agora = relogio.agora();
        Integer posicao = null;
        for (int i = 0; i < fila.size(); i++) {
            if (fila.get(i).id().equals(a.id())) {
                posicao = i + 1;
            }
        }
        return new Respostas.AtendimentoResumo(a.id(), a.alunoId(), nome, a.fichaId(), a.exercicio(), a.motivo(),
                a.status().name(), a.instrutor(), a.orientacao(), a.abertaEm(), a.concluidaEm(), a.tipo().name(),
                a.tipo().prioridade(), a.prazoDePrimeiraResposta(), a.atrasado(agora), a.minutosDeEspera(agora),
                posicao, a.transferencias(), a.nota(), a.comentario(), a.automatico());
    }
}
