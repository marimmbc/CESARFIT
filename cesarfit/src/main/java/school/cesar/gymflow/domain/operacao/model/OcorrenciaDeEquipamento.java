package school.cesar.gymflow.domain.operacao.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Texto;
import school.cesar.gymflow.domain.shared.evento.RaizDeAgregado;
import school.cesar.gymflow.domain.operacao.evento.EquipamentoInterditado;
import school.cesar.gymflow.domain.operacao.evento.EquipamentoLiberado;

/**
 * Raiz de agregado: problema registrado em um equipamento.
 *
 * Regras:
 * - a severidade define o SLA (BAIXA 72h, MEDIA 48h, ALTA 24h, CRITICA 4h) e se o equipamento fica interditado;
 * - o tempo AGUARDANDO_PECA nao conta no SLA (o relogio do SLA pausa);
 * - ao resolver, a solucao e obrigatoria; o custo e obrigatorio quando houve troca de peca;
 * - uma ocorrencia resolvida pode ser reaberta em ate 7 dias (o SLA recomeca); no maximo 2 reaberturas;
 * - reincidencia (ver {@link #REINCIDENCIAS_PARA_ELEVAR}) eleva a severidade de uma nova ocorrencia.
 */
public class OcorrenciaDeEquipamento extends RaizDeAgregado {

    public static final int DIAS_PARA_REABRIR = 7;
    public static final int MAX_REABERTURAS = 2;
    public static final int REINCIDENCIAS_PARA_ELEVAR = 2;
    public static final int JANELA_DE_REINCIDENCIA_DIAS = 30;

    private Long id;
    private final String equipamento;
    private final String descricao;
    private Severidade severidade;
    private StatusOcorrencia status;
    private final LocalDateTime abertaEm;
    private LocalDateTime atualizadaEm;
    private String solucao;
    private LocalDateTime inicioDoSla;
    private long minutosPausados;
    private LocalDateTime pausadaDesde;
    private String pecaAguardada;
    private Dinheiro custoDoReparo;
    private LocalDateTime resolvidaEm;
    private int reaberturas;
    private boolean reincidente;

    public OcorrenciaDeEquipamento(Long id, String equipamento, String descricao, Severidade severidade,
                                   StatusOcorrencia status, LocalDateTime abertaEm, LocalDateTime atualizadaEm,
                                   String solucao, LocalDateTime inicioDoSla, long minutosPausados,
                                   LocalDateTime pausadaDesde, String pecaAguardada, Dinheiro custoDoReparo,
                                   LocalDateTime resolvidaEm, int reaberturas, boolean reincidente) {
        this.id = id;
        this.equipamento = Texto.obrigatorio(equipamento, 2, 80, "Equipamento");
        this.descricao = Texto.obrigatorio(descricao, 10, 500, "Descricao do problema");
        this.severidade = severidade == null ? Severidade.MEDIA : severidade;
        this.status = status == null ? StatusOcorrencia.ABERTA : status;
        this.abertaEm = abertaEm;
        this.atualizadaEm = atualizadaEm;
        this.solucao = solucao;
        this.inicioDoSla = inicioDoSla == null ? abertaEm : inicioDoSla;
        this.minutosPausados = minutosPausados;
        this.pausadaDesde = pausadaDesde;
        this.pecaAguardada = pecaAguardada;
        this.custoDoReparo = custoDoReparo;
        this.resolvidaEm = resolvidaEm;
        this.reaberturas = reaberturas;
        this.reincidente = reincidente;
    }

    /**
     * Fabrica. Se o equipamento ja teve {@link #REINCIDENCIAS_PARA_ELEVAR} ou mais ocorrencias resolvidas nos
     * ultimos 30 dias, a nova ocorrencia nasce marcada como reincidente e com severidade um nivel acima.
     */
    public static OcorrenciaDeEquipamento registrar(String equipamento, String descricao, Severidade severidade,
                                                    long resolvidasRecentes, LocalDateTime agora) {
        Severidade base = severidade == null ? Severidade.MEDIA : severidade;
        boolean reincide = resolvidasRecentes >= REINCIDENCIAS_PARA_ELEVAR;
        OcorrenciaDeEquipamento nova = new OcorrenciaDeEquipamento(null, equipamento, descricao,
                reincide ? base.elevada() : base, StatusOcorrencia.ABERTA, agora, agora, null, agora, 0, null, null,
                null, null, 0, reincide);
        nova.avisarSeInterditou();
        return nova;
    }

    private void avisarSeInterditou() {
        if (interditado()) {
            registrar(new EquipamentoInterditado(id, equipamento, severidade.name()));
        }
    }

    public void iniciarAtendimento(LocalDateTime agora) {
        if (status != StatusOcorrencia.ABERTA) {
            throw new RegraDeNegocioException("Somente ocorrencia aberta pode entrar em andamento");
        }
        status = StatusOcorrencia.EM_ANDAMENTO;
        atualizadaEm = agora;
    }

    /** Pausa o SLA enquanto a peca de reposicao nao chega. */
    public void aguardarPeca(String peca, LocalDateTime agora) {
        if (status != StatusOcorrencia.EM_ANDAMENTO) {
            throw new RegraDeNegocioException("So e possivel aguardar peca com a ocorrencia em andamento");
        }
        pecaAguardada = Texto.obrigatorio(peca, 3, 120, "Peca aguardada");
        pausadaDesde = agora;
        status = StatusOcorrencia.AGUARDANDO_PECA;
        atualizadaEm = agora;
    }

    /** A peca chegou: o SLA volta a contar descontando o tempo de espera. */
    public void retomar(LocalDateTime agora) {
        if (status != StatusOcorrencia.AGUARDANDO_PECA) {
            throw new RegraDeNegocioException("A ocorrencia nao esta aguardando peca");
        }
        minutosPausados += Duration.between(pausadaDesde, agora).toMinutes();
        pausadaDesde = null;
        status = StatusOcorrencia.EM_ANDAMENTO;
        atualizadaEm = agora;
    }

    public void resolver(String descricaoDaSolucao, Dinheiro custo, LocalDateTime agora) {
        if (status != StatusOcorrencia.EM_ANDAMENTO) {
            throw new RegraDeNegocioException("A ocorrencia precisa estar em andamento para ser resolvida");
        }
        String limpa = Texto.obrigatorio(descricaoDaSolucao, 5, 500, "Solucao");
        if (pecaAguardada != null && (custo == null || !custo.positivo())) {
            throw new RegraDeNegocioException("Informe o custo do reparo: houve troca de peca (" + pecaAguardada + ")");
        }
        if (custo != null && custo.valor().compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraDeNegocioException("Custo do reparo nao pode ser negativo");
        }
        boolean estavaInterditado = interditado();
        solucao = limpa;
        custoDoReparo = custo;
        status = StatusOcorrencia.RESOLVIDA;
        if (estavaInterditado) {
            registrar(new EquipamentoLiberado(id, equipamento));
        }
        resolvidaEm = agora;
        atualizadaEm = agora;
    }

    public void reabrir(String motivo, LocalDateTime agora) {
        if (status != StatusOcorrencia.RESOLVIDA) {
            throw new RegraDeNegocioException("Somente ocorrencia resolvida pode ser reaberta");
        }
        if (agora.isAfter(resolvidaEm.plusDays(DIAS_PARA_REABRIR))) {
            throw new RegraDeNegocioException("Prazo de " + DIAS_PARA_REABRIR
                    + " dias para reabertura expirado: registre uma nova ocorrencia");
        }
        if (reaberturas >= MAX_REABERTURAS) {
            throw new RegraDeNegocioException("Limite de " + MAX_REABERTURAS
                    + " reaberturas atingido: avalie a substituicao do equipamento");
        }
        Texto.obrigatorio(motivo, 10, 500, "Motivo da reabertura");
        reaberturas++;
        status = StatusOcorrencia.ABERTA;
        solucao = null;
        resolvidaEm = null;
        inicioDoSla = agora;
        minutosPausados = 0;
        atualizadaEm = agora;
        avisarSeInterditou();
    }

    /** Prazo final do SLA, empurrado pelo tempo em que a ocorrencia ficou aguardando peca. */
    public LocalDateTime prazoDoSla(LocalDateTime agora) {
        long pausaAtual = pausadaDesde == null ? 0 : Duration.between(pausadaDesde, agora).toMinutes();
        return inicioDoSla.plusHours(severidade.horasDeSla()).plusMinutes(minutosPausados + pausaAtual);
    }

    public boolean atrasada(LocalDateTime agora) {
        return pendente() && agora.isAfter(prazoDoSla(agora));
    }

    public boolean interditado() {
        return pendente() && severidade.interdita();
    }

    public boolean pendente() {
        return status != StatusOcorrencia.RESOLVIDA;
    }

    public boolean resolvidaDesde(LocalDateTime limite) {
        return status == StatusOcorrencia.RESOLVIDA && resolvidaEm != null && !resolvidaEm.isBefore(limite);
    }

    public boolean doEquipamento(String nome) {
        return nome != null && equipamento.equalsIgnoreCase(nome.trim());
    }

    public void atribuirId(Long id) {
        this.id = id;
    }

    public Long id() { return id; }
    public String equipamento() { return equipamento; }
    public String descricao() { return descricao; }
    public Severidade severidade() { return severidade; }
    public StatusOcorrencia status() { return status; }
    public LocalDateTime abertaEm() { return abertaEm; }
    public LocalDateTime atualizadaEm() { return atualizadaEm; }
    public String solucao() { return solucao; }
    public LocalDateTime inicioDoSla() { return inicioDoSla; }
    public long minutosPausados() { return minutosPausados; }
    public LocalDateTime pausadaDesde() { return pausadaDesde; }
    public String pecaAguardada() { return pecaAguardada; }
    public Dinheiro custoDoReparo() { return custoDoReparo; }
    public LocalDateTime resolvidaEm() { return resolvidaEm; }
    public int reaberturas() { return reaberturas; }
    public boolean reincidente() { return reincidente; }
}
