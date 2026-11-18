package school.cesar.gymflow.domain.atendimento.model;

/** Categoria do pedido de troca de exercicio: define prioridade e regras proprias. */
public enum MotivoDeSubstituicao {

    /** Prioridade maxima na fila da equipe. */
    DOR_OU_LESAO(3),
    /** Aprovado automaticamente quando o equipamento do exercicio esta interditado por ocorrencia. */
    EQUIPAMENTO_INDISPONIVEL(2),
    DIFICULDADE_TECNICA(1),
    /** Limitada a uma troca aprovada por ficha. */
    PREFERENCIA(0);

    private final int prioridade;

    MotivoDeSubstituicao(int prioridade) {
        this.prioridade = prioridade;
    }

    public int prioridade() {
        return prioridade;
    }

    public static MotivoDeSubstituicao de(String valor) {
        if (valor == null || valor.isBlank()) {
            return PREFERENCIA;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new school.cesar.gymflow.domain.shared.RegraDeNegocioException("Categoria de troca invalida: " + valor);
        }
    }
}
