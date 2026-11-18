package school.cesar.gymflow.domain.operacao.model;

/** Gravidade do problema: define o prazo de atendimento (SLA) e se o equipamento fica interditado. */
public enum Severidade {

    BAIXA(72, false),
    MEDIA(48, false),
    ALTA(24, true),
    CRITICA(4, true);

    private final int horasDeSla;
    private final boolean interdita;

    Severidade(int horasDeSla, boolean interdita) {
        this.horasDeSla = horasDeSla;
        this.interdita = interdita;
    }

    public int horasDeSla() {
        return horasDeSla;
    }

    /** ALTA e CRITICA: o equipamento nao pode ser usado ate a resolucao. */
    public boolean interdita() {
        return interdita;
    }

    /** Um nivel acima (CRITICA continua CRITICA). Usado na reincidencia. */
    public Severidade elevada() {
        return this == CRITICA ? CRITICA : values()[ordinal() + 1];
    }

    public static Severidade de(String valor) {
        if (valor == null || valor.isBlank()) {
            return MEDIA;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new school.cesar.gymflow.domain.shared.RegraDeNegocioException(
                    "Severidade invalida: use BAIXA, MEDIA, ALTA ou CRITICA");
        }
    }
}
