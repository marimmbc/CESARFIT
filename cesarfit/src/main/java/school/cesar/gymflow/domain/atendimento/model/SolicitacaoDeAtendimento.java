package school.cesar.gymflow.domain.atendimento.model;

import java.time.Duration;
import java.time.LocalDateTime;

import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Texto;

/**
 * Raiz de agregado: pedido de ajuda ao instrutor vinculado a ficha (e opcionalmente a um exercicio).
 *
 * ABERTO -> EM_ATENDIMENTO (-> transferido para outro instrutor, ate 2x) -> CONCLUIDO -> avaliado pelo aluno;
 * ABERTO -> CANCELADO (pelo aluno).
 * Regras: SLA de primeira resposta (10 min prioritario / 30 min normal); avaliacao do aluno de 1 a 5, uma unica
 * vez, ate 48h apos a conclusao, com comentario obrigatorio para notas 1 e 2.
 */
public class SolicitacaoDeAtendimento {

    public static final int MAX_TRANSFERENCIAS = 2;
    public static final int HORAS_PARA_AVALIAR = 48;

    private Long id;
    private final Long alunoId;
    private final Long fichaId;
    private final String exercicio;
    private final String motivo;
    private final TipoDeAtendimento tipo;
    private StatusAtendimento status;
    private String instrutor;
    private String orientacao;
    private final LocalDateTime abertaEm;
    private LocalDateTime assumidoEm;
    private LocalDateTime concluidaEm;
    private int transferencias;
    private Integer nota;
    private String comentario;
    private final boolean automatico;

    public SolicitacaoDeAtendimento(Long id, Long alunoId, Long fichaId, String exercicio, String motivo,
                                    TipoDeAtendimento tipo, StatusAtendimento status, String instrutor,
                                    String orientacao, LocalDateTime abertaEm, LocalDateTime assumidoEm,
                                    LocalDateTime concluidaEm, int transferencias, Integer nota, String comentario,
                                    boolean automatico) {
        if (fichaId == null) {
            throw new RegraDeNegocioException("O atendimento deve estar vinculado a uma ficha de treino");
        }
        this.id = id;
        this.alunoId = alunoId;
        this.fichaId = fichaId;
        this.exercicio = exercicio;
        this.motivo = Texto.obrigatorio(motivo, 10, 500, "Motivo do atendimento");
        this.tipo = tipo == null ? TipoDeAtendimento.DUVIDA_EXECUCAO : tipo;
        this.status = status == null ? StatusAtendimento.ABERTO : status;
        this.instrutor = instrutor;
        this.orientacao = orientacao;
        this.abertaEm = abertaEm;
        this.assumidoEm = assumidoEm;
        this.concluidaEm = concluidaEm;
        this.transferencias = transferencias;
        this.nota = nota;
        this.comentario = comentario;
        this.automatico = automatico;
    }

    public static SolicitacaoDeAtendimento abrir(Long alunoId, Long fichaId, String exercicio, String motivo,
                                                 TipoDeAtendimento tipo, boolean automatico, LocalDateTime agora) {
        return new SolicitacaoDeAtendimento(null, alunoId, fichaId, exercicio, motivo, tipo,
                StatusAtendimento.ABERTO, null, null, agora, null, null, 0, null, null, automatico);
    }

    public void assumir(String nomeDoInstrutor, LocalDateTime agora) {
        if (status != StatusAtendimento.ABERTO) {
            throw new RegraDeNegocioException("Somente atendimento aberto pode ser assumido");
        }
        instrutor = Texto.obrigatorio(nomeDoInstrutor, 2, 80, "Instrutor");
        assumidoEm = agora;
        status = StatusAtendimento.EM_ATENDIMENTO;
    }

    public void transferir(String novoInstrutor) {
        if (status != StatusAtendimento.EM_ATENDIMENTO) {
            throw new RegraDeNegocioException("Somente atendimento em andamento pode ser transferido");
        }
        String destino = Texto.obrigatorio(novoInstrutor, 2, 80, "Instrutor de destino");
        if (destino.equalsIgnoreCase(instrutor)) {
            throw new RegraDeNegocioException("Escolha um instrutor diferente do atual");
        }
        if (transferencias >= MAX_TRANSFERENCIAS) {
            throw new RegraDeNegocioException("Limite de " + MAX_TRANSFERENCIAS + " transferencias atingido");
        }
        instrutor = destino;
        transferencias++;
    }

    public void concluir(String orientacaoDoInstrutor, LocalDateTime agora) {
        if (status != StatusAtendimento.EM_ATENDIMENTO) {
            throw new RegraDeNegocioException("O atendimento precisa ser assumido antes de ser concluido");
        }
        orientacao = Texto.obrigatorio(orientacaoDoInstrutor, 10, 500, "Orientacao do instrutor");
        status = StatusAtendimento.CONCLUIDO;
        concluidaEm = agora;
    }

    public void cancelar() {
        if (status != StatusAtendimento.ABERTO) {
            throw new RegraDeNegocioException("So e possivel cancelar enquanto o instrutor nao assumiu");
        }
        status = StatusAtendimento.CANCELADO;
    }

    public void avaliar(int notaDoAluno, String comentarioDoAluno, LocalDateTime agora) {
        if (status != StatusAtendimento.CONCLUIDO) {
            throw new RegraDeNegocioException("So e possivel avaliar um atendimento concluido");
        }
        if (nota != null) {
            throw new RegraDeNegocioException("Este atendimento ja foi avaliado");
        }
        if (agora.isAfter(concluidaEm.plusHours(HORAS_PARA_AVALIAR))) {
            throw new RegraDeNegocioException("Prazo de " + HORAS_PARA_AVALIAR + " horas para avaliar expirado");
        }
        if (notaDoAluno < 1 || notaDoAluno > 5) {
            throw new RegraDeNegocioException("A nota deve ser de 1 a 5");
        }
        comentario = notaDoAluno <= 2
                ? Texto.obrigatorio(comentarioDoAluno, 10, 300, "Comentario (obrigatorio para nota 1 ou 2)")
                : Texto.opcional(comentarioDoAluno, 300, "Comentario");
        nota = notaDoAluno;
    }

    public LocalDateTime prazoDePrimeiraResposta() {
        return abertaEm.plusMinutes(tipo.minutosDeSla());
    }

    public boolean atrasado(LocalDateTime agora) {
        return status == StatusAtendimento.ABERTO && agora.isAfter(prazoDePrimeiraResposta());
    }

    public long minutosDeEspera(LocalDateTime agora) {
        LocalDateTime fim = assumidoEm != null ? assumidoEm : status == StatusAtendimento.ABERTO ? agora : abertaEm;
        return Math.max(0, Duration.between(abertaEm, fim).toMinutes());
    }

    public boolean emAberto() {
        return status == StatusAtendimento.ABERTO || status == StatusAtendimento.EM_ATENDIMENTO;
    }

    public boolean aguardando() {
        return status == StatusAtendimento.ABERTO;
    }

    public boolean emAndamentoCom(String nomeDoInstrutor) {
        return status == StatusAtendimento.EM_ATENDIMENTO && instrutor != null
                && instrutor.equalsIgnoreCase(nomeDoInstrutor == null ? "" : nomeDoInstrutor.trim());
    }

    public void atribuirId(Long id) {
        this.id = id;
    }

    public Long id() { return id; }
    public Long alunoId() { return alunoId; }
    public Long fichaId() { return fichaId; }
    public String exercicio() { return exercicio; }
    public String motivo() { return motivo; }
    public TipoDeAtendimento tipo() { return tipo; }
    public StatusAtendimento status() { return status; }
    public String instrutor() { return instrutor; }
    public String orientacao() { return orientacao; }
    public LocalDateTime abertaEm() { return abertaEm; }
    public LocalDateTime assumidoEm() { return assumidoEm; }
    public LocalDateTime concluidaEm() { return concluidaEm; }
    public int transferencias() { return transferencias; }
    public Integer nota() { return nota; }
    public String comentario() { return comentario; }
    public boolean automatico() { return automatico; }
}
