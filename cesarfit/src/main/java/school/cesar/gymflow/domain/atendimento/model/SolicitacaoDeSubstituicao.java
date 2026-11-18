package school.cesar.gymflow.domain.atendimento.model;

import java.time.LocalDateTime;

import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Texto;

/**
 * Raiz de agregado: o aluno pede para trocar um exercicio da ficha; a equipe aprova ou rejeita
 * (ou o sistema aprova automaticamente quando o equipamento esta interditado).
 * Pedido pendente ha mais de 7 dias expira e nao pode mais ser respondido.
 */
public class SolicitacaoDeSubstituicao {

    public static final int DIAS_PARA_EXPIRAR = 7;

    private Long id;
    private final Long alunoId;
    private final Long fichaId;
    private final int posicao;
    private final String exercicioAtual;
    private final String motivo;
    private final MotivoDeSubstituicao categoria;
    private StatusSolicitacao status;
    private String exercicioNovo;
    private String resposta;
    private final LocalDateTime criadaEm;
    private LocalDateTime respondidaEm;
    private boolean automatica;

    public SolicitacaoDeSubstituicao(Long id, Long alunoId, Long fichaId, int posicao, String exercicioAtual,
                                     String motivo, MotivoDeSubstituicao categoria, StatusSolicitacao status,
                                     String exercicioNovo, String resposta, LocalDateTime criadaEm,
                                     LocalDateTime respondidaEm, boolean automatica) {
        this.id = id;
        this.alunoId = alunoId;
        this.fichaId = fichaId;
        this.posicao = posicao;
        this.exercicioAtual = exercicioAtual;
        this.motivo = Texto.obrigatorio(motivo, 10, 500, "Motivo da substituicao");
        this.categoria = categoria == null ? MotivoDeSubstituicao.PREFERENCIA : categoria;
        this.status = status == null ? StatusSolicitacao.PENDENTE : status;
        this.exercicioNovo = exercicioNovo;
        this.resposta = resposta;
        this.criadaEm = criadaEm;
        this.respondidaEm = respondidaEm;
        this.automatica = automatica;
    }

    public static SolicitacaoDeSubstituicao solicitar(Long alunoId, Long fichaId, int posicao, String exercicioAtual,
                                                      String motivo, MotivoDeSubstituicao categoria,
                                                      LocalDateTime agora) {
        return new SolicitacaoDeSubstituicao(null, alunoId, fichaId, posicao, exercicioAtual, motivo, categoria,
                StatusSolicitacao.PENDENTE, null, null, agora, null, false);
    }

    public void aprovar(String novoExercicio, String obs, boolean pelaRegraAutomatica, LocalDateTime agora) {
        exigirPendente(agora);
        exercicioNovo = Texto.obrigatorio(novoExercicio, 2, 60, "Exercicio substituto");
        resposta = Texto.opcional(obs, 300, "Resposta");
        automatica = pelaRegraAutomatica;
        status = StatusSolicitacao.APROVADA;
        respondidaEm = agora;
    }

    public void rejeitar(String justificativa, LocalDateTime agora) {
        exigirPendente(agora);
        resposta = Texto.obrigatorio(justificativa, 10, 300, "Justificativa da rejeicao");
        status = StatusSolicitacao.REJEITADA;
        respondidaEm = agora;
    }

    private void exigirPendente(LocalDateTime agora) {
        if (status != StatusSolicitacao.PENDENTE) {
            throw new RegraDeNegocioException("Solicitacao ja foi respondida");
        }
        if (expirada(agora)) {
            throw new RegraDeNegocioException("Solicitacao expirada: o aluno deve fazer um novo pedido");
        }
    }

    public boolean expirada(LocalDateTime agora) {
        return status == StatusSolicitacao.PENDENTE && agora.isAfter(criadaEm.plusDays(DIAS_PARA_EXPIRAR));
    }

    /** Situacao considerando a expiracao. */
    public StatusSolicitacao statusEm(LocalDateTime agora) {
        return expirada(agora) ? StatusSolicitacao.EXPIRADA : status;
    }

    public boolean pendente(LocalDateTime agora) {
        return statusEm(agora) == StatusSolicitacao.PENDENTE;
    }

    public boolean aprovada() {
        return status == StatusSolicitacao.APROVADA;
    }

    public void atribuirId(Long id) {
        this.id = id;
    }

    public Long id() { return id; }
    public Long alunoId() { return alunoId; }
    public Long fichaId() { return fichaId; }
    public int posicao() { return posicao; }
    public String exercicioAtual() { return exercicioAtual; }
    public String motivo() { return motivo; }
    public MotivoDeSubstituicao categoria() { return categoria; }
    public StatusSolicitacao status() { return status; }
    public String exercicioNovo() { return exercicioNovo; }
    public String resposta() { return resposta; }
    public LocalDateTime criadaEm() { return criadaEm; }
    public LocalDateTime respondidaEm() { return respondidaEm; }
    public boolean automatica() { return automatica; }
}
