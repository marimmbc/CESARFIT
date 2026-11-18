package school.cesar.gymflow.domain.matricula.model;

import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

/** Formas aceitas pela academia. So dinheiro devolve troco; so cartao de credito parcela. */
public enum FormaDePagamento {
    PIX("PIX"),
    DINHEIRO("Dinheiro"),
    CARTAO_DEBITO("Cartao de debito"),
    CARTAO_CREDITO("Cartao de credito"),
    BOLETO("Boleto");

    public static final int MAX_PARCELAS = 12;

    private final String nome;

    FormaDePagamento(String nome) {
        this.nome = nome;
    }

    public String nome() {
        return nome;
    }

    public boolean aceitaTroco() {
        return this == DINHEIRO;
    }

    public boolean parcelavel() {
        return this == CARTAO_CREDITO;
    }

    /** Aceita tambem "CARTAO" (telas antigas) como cartao de credito. */
    public static FormaDePagamento de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new RegraDeNegocioException("Informe a forma de pagamento");
        }
        String normalizado = texto.trim().toUpperCase().replace(' ', '_');
        if (normalizado.equals("CARTAO")) {
            return CARTAO_CREDITO;
        }
        try {
            return valueOf(normalizado);
        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException("Forma de pagamento invalida: " + texto
                    + ". Use PIX, DINHEIRO, CARTAO_DEBITO, CARTAO_CREDITO ou BOLETO");
        }
    }
}
