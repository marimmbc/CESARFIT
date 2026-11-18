package school.cesar.gymflow.application.usecase;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.financeiro.model.ContestacaoDeCobranca;
import school.cesar.gymflow.domain.financeiro.model.StatusContestacao;
import school.cesar.gymflow.domain.financeiro.repositorio.RepositorioDeContestacoes;
import school.cesar.gymflow.domain.matricula.model.Matricula;
import school.cesar.gymflow.domain.matricula.model.Pagamento;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeMatriculas;
import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;
import school.cesar.gymflow.domain.shared.evento.BarramentoDeEventos;

/**
 * Funcionalidade: contestacao de cobranca com pedido de informacoes, resultado parcial,
 * SLA de analise, recurso unico julgado por outro analista e limite de contestacoes improcedentes.
 * O resultado procedente/parcial estorna o pagamento automaticamente (categoria CONTESTACAO_PROCEDENTE).
 */
@Service
public class ContestacaoDeCobrancaUseCase {

    private final RepositorioDeContestacoes contestacoes;
    private final RepositorioDeMatriculas matriculas;
    private final NomesDeAlunos nomes;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;
    private final BarramentoDeEventos barramento;

    public ContestacaoDeCobrancaUseCase(RepositorioDeContestacoes contestacoes, RepositorioDeMatriculas matriculas,
                                        NomesDeAlunos nomes, PublicadorDeEventos publicador, Relogio relogio,
                                        BarramentoDeEventos barramento) {
        this.barramento = barramento;
        this.contestacoes = contestacoes;
        this.matriculas = matriculas;
        this.nomes = nomes;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public Respostas.ContestacaoResumo abrir(Long alunoId, Comandos.AbrirContestacao comando) {
        LocalDateTime agora = relogio.agora();
        Matricula matricula = matriculas.porId(comando.matriculaId())
                .filter(m -> m.alunoId().equals(alunoId))
                .orElseThrow(() -> new RegraDeNegocioException("Matricula nao encontrada para este aluno"));
        Pagamento pagamento = matricula.pagamento(comando.indiceDoPagamento());
        if (pagamento.estornado()) {
            throw new RegraDeNegocioException("Pagamento estornado nao pode ser contestado");
        }
        if (ChronoUnit.DAYS.between(pagamento.data(), agora.toLocalDate()) > ContestacaoDeCobranca.DIAS_PARA_CONTESTAR) {
            throw new RegraDeNegocioException("Prazo de " + ContestacaoDeCobranca.DIAS_PARA_CONTESTAR
                    + " dias para contestar este pagamento expirado");
        }
        List<ContestacaoDeCobranca> doAluno = contestacoes.porAluno(alunoId);
        boolean jaAberta = doAluno.stream().anyMatch(c -> c.aberta()
                && c.matriculaId().equals(matricula.id()) && c.indiceDoPagamento() == comando.indiceDoPagamento());
        if (jaAberta) {
            throw new RegraDeNegocioException("Ja existe uma contestacao em analise para este pagamento");
        }
        LocalDateTime janela = agora.minusDays(ContestacaoDeCobranca.JANELA_DE_IMPROCEDENTES_DIAS);
        if (doAluno.stream().filter(c -> c.improcedenteDesde(janela)).count() >= ContestacaoDeCobranca.LIMITE_IMPROCEDENTES) {
            throw new RegraDeNegocioException("Limite de contestacoes improcedentes atingido nos ultimos "
                    + ContestacaoDeCobranca.JANELA_DE_IMPROCEDENTES_DIAS + " dias: procure a recepcao");
        }
        ContestacaoDeCobranca salva = contestacoes.salvar(ContestacaoDeCobranca.abrir(matricula.id(), alunoId,
                comando.indiceDoPagamento(), pagamento.saldoEstornavel().toString(), comando.motivo(), agora));
        publicar(salva, "Cobranca de " + salva.valor() + " contestada: " + salva.motivo());
        return resumo(salva, nomes.de(alunoId), agora);
    }

    @Transactional
    public Respostas.ContestacaoResumo pedirInformacoes(Long id, String pergunta) {
        ContestacaoDeCobranca c = buscar(id);
        c.pedirInformacoes(pergunta);
        return salvar(c, "Equipe pediu informacoes: " + c.pergunta());
    }

    @Transactional
    public Respostas.ContestacaoResumo responder(Long alunoId, Long id, String resposta) {
        ContestacaoDeCobranca c = doAluno(alunoId, id);
        c.responder(resposta);
        return salvar(c, "Aluno respondeu: " + c.respostaDoAluno());
    }

    @Transactional
    public Respostas.ContestacaoResumo recorrer(Long alunoId, Long id, String justificativa) {
        ContestacaoDeCobranca c = doAluno(alunoId, id);
        c.recorrer(justificativa, relogio.agora());
        return salvar(c, "Recurso aberto: " + c.justificativaDoRecurso());
    }

    @Transactional
    public Respostas.ContestacaoResumo analisar(Long id, Comandos.AnalisarContestacao comando) {
        return analisar(id, comando, "Equipe");
    }

    @Transactional
    public Respostas.ContestacaoResumo analisar(Long id, Comandos.AnalisarContestacao comando, String analista) {
        ContestacaoDeCobranca contestacao = buscar(id);
        StatusContestacao resultado = comando.resultado() != null && !comando.resultado().isBlank()
                ? StatusContestacao.resultado(comando.resultado())
                : Boolean.TRUE.equals(comando.procedente()) ? StatusContestacao.PROCEDENTE : StatusContestacao.IMPROCEDENTE;
        Dinheiro parcial = comando.valorParcial() == null || comando.valorParcial().isBlank()
                ? null : Dinheiro.de(comando.valorParcial());
        boolean eraRecurso = contestacao.status() == StatusContestacao.EM_RECURSO;
        contestacao.analisar(resultado, parcial, comando.parecer(), analista, relogio.agora());
        // O estorno e feito pelo contexto Matricula, que reage ao evento ContestacaoJulgadaProcedente.
        barramento.postarTodos(contestacao.retirarEventos());
        return salvar(contestacao, (eraRecurso ? "Recurso julgado " : "Contestacao ") + contestacao.status()
                + " por " + contestacao.analista() + ": " + contestacao.parecer());
    }

    @Transactional(readOnly = true)
    public List<Respostas.ContestacaoResumo> doAluno(Long alunoId) {
        String nome = nomes.de(alunoId);
        LocalDateTime agora = relogio.agora();
        return contestacoes.porAluno(alunoId).stream().map(c -> resumo(c, nome, agora)).toList();
    }

    @Transactional(readOnly = true)
    public List<Respostas.ContestacaoResumo> todas() {
        Map<Long, String> mapa = nomes.mapa();
        LocalDateTime agora = relogio.agora();
        return contestacoes.todas().stream().map(c -> resumo(c, mapa.getOrDefault(c.alunoId(), "-"), agora)).toList();
    }

    private ContestacaoDeCobranca buscar(Long id) {
        return contestacoes.porId(id).orElseThrow(() -> new RegraDeNegocioException("Contestacao nao encontrada"));
    }

    private ContestacaoDeCobranca doAluno(Long alunoId, Long id) {
        ContestacaoDeCobranca c = buscar(id);
        if (!c.alunoId().equals(alunoId)) {
            throw new RegraDeNegocioException("Contestacao nao encontrada");
        }
        return c;
    }

    private Respostas.ContestacaoResumo salvar(ContestacaoDeCobranca c, String detalhe) {
        ContestacaoDeCobranca salva = contestacoes.salvar(c);
        publicar(salva, detalhe);
        return resumo(salva, nomes.de(salva.alunoId()), relogio.agora());
    }

    private void publicar(ContestacaoDeCobranca c, String detalhe) {
        publicador.publicar(EventoDeDominio.de(EventoDeDominio.CONTESTACAO_COBRANCA, String.valueOf(c.id()), detalhe));
    }

    static Respostas.ContestacaoResumo resumo(ContestacaoDeCobranca c, String nome, LocalDateTime agora) {
        return new Respostas.ContestacaoResumo(c.id(), c.matriculaId(), c.alunoId(), nome, c.indiceDoPagamento(),
                c.valor(), c.motivo(), c.status().name(), c.parecer(), c.abertaEm(), c.analisadaEm(), c.analista(),
                c.valorEstornado(), c.pergunta(), c.respostaDoAluno(), c.recursoUsado(), c.justificativaDoRecurso(),
                c.prazoDaAnalise(), c.atrasada(agora));
    }
}
