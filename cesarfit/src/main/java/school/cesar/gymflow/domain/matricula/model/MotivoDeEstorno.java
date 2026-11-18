package school.cesar.gymflow.domain.matricula.model;

/** Categoria do estorno: cada uma tem uma regra propria de elegibilidade (ver Matricula.estornarPagamento). */
public enum MotivoDeEstorno {
    /** Mesmo valor cobrado duas vezes em ate 3 dias. Estorno integral. */
    COBRANCA_DUPLICADA,
    /** Valor cobrado diferente do devido. Aceita estorno parcial. */
    VALOR_INCORRETO,
    /** Direito de arrependimento (CDC art. 49): ate 7 dias do pagamento, integral, cancela a matricula sem taxa. */
    DESISTENCIA,
    /** Gerado pela analise de uma contestacao de cobranca. */
    CONTESTACAO_PROCEDENTE,
    OUTRO;

    public static MotivoDeEstorno de(String valor) {
        if (valor == null || valor.isBlank()) {
            return OUTRO;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new school.cesar.gymflow.domain.shared.RegraDeNegocioException("Categoria de estorno invalida: " + valor);
        }
    }
}
