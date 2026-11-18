package school.cesar.gymflow.domain.atendimento.model;

/** Tipo do chamado: dor/lesao tem prioridade ALTA e SLA de primeira resposta menor. */
public enum TipoDeAtendimento {

    DUVIDA_EXECUCAO(false),
    AJUSTE_DE_EQUIPAMENTO(false),
    DOR_OU_LESAO(true),
    OUTRO(false);

    public static final int MINUTOS_SLA_PRIORITARIO = 10;
    public static final int MINUTOS_SLA_NORMAL = 30;

    private final boolean prioritario;

    TipoDeAtendimento(boolean prioritario) {
        this.prioritario = prioritario;
    }

    public boolean prioritario() {
        return prioritario;
    }

    public String prioridade() {
        return prioritario ? "ALTA" : "NORMAL";
    }

    public int minutosDeSla() {
        return prioritario ? MINUTOS_SLA_PRIORITARIO : MINUTOS_SLA_NORMAL;
    }

    public static TipoDeAtendimento de(String valor) {
        if (valor == null || valor.isBlank()) {
            return DUVIDA_EXECUCAO;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new school.cesar.gymflow.domain.shared.RegraDeNegocioException("Tipo de atendimento invalido: " + valor);
        }
    }
}
