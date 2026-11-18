package school.cesar.gymflow.domain.financeiro.model;

/**
 * ABERTA -> (AGUARDANDO_ALUNO -> ABERTA)* -> PROCEDENTE | PARCIALMENTE_PROCEDENTE | IMPROCEDENTE
 * IMPROCEDENTE | PARCIALMENTE_PROCEDENTE -> EM_RECURSO (uma vez) -> decisao final.
 */
public enum StatusContestacao {
    ABERTA,
    AGUARDANDO_ALUNO,
    PROCEDENTE,
    PARCIALMENTE_PROCEDENTE,
    IMPROCEDENTE,
    EM_RECURSO;

    public boolean emAberto() {
        return this == ABERTA || this == AGUARDANDO_ALUNO || this == EM_RECURSO;
    }

    public static StatusContestacao resultado(String valor) {
        try {
            StatusContestacao s = valueOf(valor.trim().toUpperCase());
            if (s == PROCEDENTE || s == PARCIALMENTE_PROCEDENTE || s == IMPROCEDENTE) {
                return s;
            }
        } catch (RuntimeException e) {
            // cai na mensagem abaixo
        }
        throw new school.cesar.gymflow.domain.shared.RegraDeNegocioException(
                "Resultado invalido: use PROCEDENTE, PARCIALMENTE_PROCEDENTE ou IMPROCEDENTE");
    }
}
