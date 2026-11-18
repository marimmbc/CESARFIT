package school.cesar.gymflow.domain.financeiro.model;

import java.time.LocalDateTime;

import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Texto;

/**
 * Raiz de agregado: o aluno contesta um pagamento cobrado; a equipe analisa.
 *
 * Regras do agregado:
 * - a equipe pode pedir informacoes ao aluno (no maximo 2 vezes); enquanto aguarda, nao pode julgar;
 * - o resultado pode ser procedente (estorno do saldo), parcialmente procedente (estorno de parte do valor,
 *   menor que o pagamento) ou improcedente;
 * - SLA de analise: 5 dias a partir da abertura (ou do recurso);
 * - de uma decisao improcedente/parcial cabe UM recurso, em ate 7 dias, julgado por OUTRO analista.
 */
public class ContestacaoDeCobranca extends school.cesar.gymflow.domain.shared.evento.RaizDeAgregado {

    public static final int DIAS_PARA_CONTESTAR = 30;
    public static final int DIAS_DE_SLA = 5;
    public static final int DIAS_PARA_RECORRER = 7;
    public static final int MAX_PEDIDOS_DE_INFORMACAO = 2;
    public static final int LIMITE_IMPROCEDENTES = 2;
    public static final int JANELA_DE_IMPROCEDENTES_DIAS = 180;

    private Long id;
    private final Long matriculaId;
    private final Long alunoId;
    private final int indiceDoPagamento;
    private final String valor;
    private final String motivo;
    private StatusContestacao status;
    private String parecer;
    private final LocalDateTime abertaEm;
    private LocalDateTime analisadaEm;
    private String analista;
    private String valorEstornado;
    private String pergunta;
    private String respostaDoAluno;
    private int pedidosDeInformacao;
    private String justificativaDoRecurso;
    private LocalDateTime recorridaEm;
    private boolean recursoUsado;

    public ContestacaoDeCobranca(Long id, Long matriculaId, Long alunoId, int indiceDoPagamento, String valor,
                                 String motivo, StatusContestacao status, String parecer,
                                 LocalDateTime abertaEm, LocalDateTime analisadaEm, String analista,
                                 String valorEstornado, String pergunta, String respostaDoAluno,
                                 int pedidosDeInformacao, String justificativaDoRecurso,
                                 LocalDateTime recorridaEm, boolean recursoUsado) {
        this.id = id;
        this.matriculaId = matriculaId;
        this.alunoId = alunoId;
        this.indiceDoPagamento = indiceDoPagamento;
        this.valor = valor;
        this.motivo = Texto.obrigatorio(motivo, 10, 500, "Motivo da contestacao");
        this.status = status == null ? StatusContestacao.ABERTA : status;
        this.parecer = parecer;
        this.abertaEm = abertaEm;
        this.analisadaEm = analisadaEm;
        this.analista = analista;
        this.valorEstornado = valorEstornado;
        this.pergunta = pergunta;
        this.respostaDoAluno = respostaDoAluno;
        this.pedidosDeInformacao = pedidosDeInformacao;
        this.justificativaDoRecurso = justificativaDoRecurso;
        this.recorridaEm = recorridaEm;
        this.recursoUsado = recursoUsado;
    }

    public static ContestacaoDeCobranca abrir(Long matriculaId, Long alunoId, int indice, String valor, String motivo,
                                              LocalDateTime agora) {
        return new ContestacaoDeCobranca(null, matriculaId, alunoId, indice, valor, motivo,
                StatusContestacao.ABERTA, null, agora, null, null, null, null, null, 0, null, null, false);
    }

    public void pedirInformacoes(String perguntaDaEquipe) {
        if (status != StatusContestacao.ABERTA && status != StatusContestacao.EM_RECURSO) {
            throw new RegraDeNegocioException("So e possivel pedir informacoes de uma contestacao em analise");
        }
        if (pedidosDeInformacao >= MAX_PEDIDOS_DE_INFORMACAO) {
            throw new RegraDeNegocioException("Limite de " + MAX_PEDIDOS_DE_INFORMACAO
                    + " pedidos de informacao atingido: decida com o que ja foi enviado");
        }
        pergunta = Texto.obrigatorio(perguntaDaEquipe, 10, 300, "Pergunta ao aluno");
        respostaDoAluno = null;
        pedidosDeInformacao++;
        status = StatusContestacao.AGUARDANDO_ALUNO;
    }

    public void responder(String resposta) {
        if (status != StatusContestacao.AGUARDANDO_ALUNO) {
            throw new RegraDeNegocioException("A equipe nao pediu informacoes nesta contestacao");
        }
        respostaDoAluno = Texto.obrigatorio(resposta, 5, 500, "Resposta do aluno");
        status = recursoUsado ? StatusContestacao.EM_RECURSO : StatusContestacao.ABERTA;
    }

    /**
     * Registra a decisao. Valida a transicao, o valor parcial e o analista do recurso.
     * Retorna o valor a estornar (null = saldo integral) ou Dinheiro.ZERO se nada deve ser estornado.
     */
    public Dinheiro analisar(StatusContestacao resultado, Dinheiro valorParcial, String parecerDaEquipe,
                             String nomeDoAnalista, LocalDateTime agora) {
        if (status == StatusContestacao.AGUARDANDO_ALUNO) {
            throw new RegraDeNegocioException("Contestacao aguardando resposta do aluno");
        }
        if (status != StatusContestacao.ABERTA && status != StatusContestacao.EM_RECURSO) {
            throw new RegraDeNegocioException("Contestacao ja foi analisada");
        }
        String quem = Texto.obrigatorio(nomeDoAnalista, 2, 80, "Analista");
        if (status == StatusContestacao.EM_RECURSO && quem.equalsIgnoreCase(analista)) {
            throw new RegraDeNegocioException("O recurso deve ser julgado por outro analista");
        }
        String texto = Texto.obrigatorio(parecerDaEquipe, 10, 500, "Parecer da analise");
        Dinheiro aEstornar;
        switch (resultado) {
            case PROCEDENTE -> aEstornar = null;
            case PARCIALMENTE_PROCEDENTE -> {
                Dinheiro cobrado = Dinheiro.de(valor);
                if (valorParcial == null || !valorParcial.positivo() || !cobrado.maiorQue(valorParcial)) {
                    throw new RegraDeNegocioException(
                            "No resultado parcial, informe um valor a estornar maior que zero e menor que " + cobrado);
                }
                aEstornar = valorParcial;
            }
            case IMPROCEDENTE -> aEstornar = Dinheiro.ZERO;
            default -> throw new RegraDeNegocioException("Resultado invalido");
        }
        parecer = texto;
        analista = quem;
        status = resultado;
        analisadaEm = agora;
        valorEstornado = aEstornar == null ? valor : aEstornar.zero() ? null : aEstornar.toString();
        if (aEstornar == null || aEstornar.positivo()) {
            registrar(new school.cesar.gymflow.domain.financeiro.evento.ContestacaoJulgadaProcedente(
                    id, matriculaId, indiceDoPagamento, aEstornar, resultado.name(), parecer));
        }
        return aEstornar;
    }

    public void recorrer(String justificativa, LocalDateTime agora) {
        if (status != StatusContestacao.IMPROCEDENTE && status != StatusContestacao.PARCIALMENTE_PROCEDENTE) {
            throw new RegraDeNegocioException("So cabe recurso de decisao improcedente ou parcialmente procedente");
        }
        if (recursoUsado) {
            throw new RegraDeNegocioException("Recurso ja utilizado nesta contestacao");
        }
        if (agora.isAfter(analisadaEm.plusDays(DIAS_PARA_RECORRER))) {
            throw new RegraDeNegocioException("Prazo de " + DIAS_PARA_RECORRER + " dias para recurso expirado");
        }
        justificativaDoRecurso = Texto.obrigatorio(justificativa, 20, 500, "Justificativa do recurso");
        recursoUsado = true;
        recorridaEm = agora;
        status = StatusContestacao.EM_RECURSO;
    }

    public LocalDateTime prazoDaAnalise() {
        return (recorridaEm != null ? recorridaEm : abertaEm).plusDays(DIAS_DE_SLA);
    }

    public boolean atrasada(LocalDateTime agora) {
        return (status == StatusContestacao.ABERTA || status == StatusContestacao.EM_RECURSO)
                && agora.isAfter(prazoDaAnalise());
    }

    public boolean aberta() {
        return status.emAberto();
    }

    public boolean improcedenteDesde(LocalDateTime limite) {
        return status == StatusContestacao.IMPROCEDENTE && analisadaEm != null && !analisadaEm.isBefore(limite);
    }

    public void atribuirId(Long id) {
        this.id = id;
    }

    public Long id() { return id; }
    public Long matriculaId() { return matriculaId; }
    public Long alunoId() { return alunoId; }
    public int indiceDoPagamento() { return indiceDoPagamento; }
    public String valor() { return valor; }
    public String motivo() { return motivo; }
    public StatusContestacao status() { return status; }
    public String parecer() { return parecer; }
    public LocalDateTime abertaEm() { return abertaEm; }
    public LocalDateTime analisadaEm() { return analisadaEm; }
    public String analista() { return analista; }
    public String valorEstornado() { return valorEstornado; }
    public String pergunta() { return pergunta; }
    public String respostaDoAluno() { return respostaDoAluno; }
    public int pedidosDeInformacao() { return pedidosDeInformacao; }
    public String justificativaDoRecurso() { return justificativaDoRecurso; }
    public LocalDateTime recorridaEm() { return recorridaEm; }
    public boolean recursoUsado() { return recursoUsado; }
}
