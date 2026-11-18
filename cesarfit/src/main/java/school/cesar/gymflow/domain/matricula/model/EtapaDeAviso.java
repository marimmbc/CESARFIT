package school.cesar.gymflow.domain.matricula.model;

/**
 * Regua de comunicacao do vencimento. Cada etapa tem uma janela em dias relativa ao fim da matricula
 * (positivo = antes do vencimento, negativo = depois). Etapas sao enviadas uma unica vez por vencimento
 * e sempre em ordem crescente: o sistema nunca "volta" para uma etapa anterior.
 */
public enum EtapaDeAviso {

    LEMBRETE(7, 4, false, "Lembrete"),
    PROXIMO_DO_VENCIMENTO(3, 1, false, "Vence em breve"),
    VENCE_HOJE(0, 0, false, "Vence hoje"),
    VENCIDA(-1, -4, true, "Matricula vencida"),
    ULTIMO_AVISO(-5, -15, true, "Ultimo aviso");

    /** Planos com vigencia menor que isso nao recebem o lembrete de 7 dias (seria no proprio dia do inicio). */
    public static final int VIGENCIA_MINIMA_PARA_LEMBRETE = 15;

    private final int deDias;
    private final int ateDias;
    private final boolean cobranca;
    private final String rotulo;

    EtapaDeAviso(int deDias, int ateDias, boolean cobranca, String rotulo) {
        this.deDias = deDias;
        this.ateDias = ateDias;
        this.cobranca = cobranca;
        this.rotulo = rotulo;
    }

    /** Etapa cuja janela contem a distancia (em dias) ate o fim; null se fora da regua. */
    public static EtapaDeAviso para(long diasAteOFim) {
        for (EtapaDeAviso etapa : values()) {
            if (diasAteOFim <= etapa.deDias && diasAteOFim >= etapa.ateDias) {
                return etapa;
            }
        }
        return null;
    }

    public boolean cobranca() {
        return cobranca;
    }

    public String rotulo() {
        return rotulo;
    }
}
