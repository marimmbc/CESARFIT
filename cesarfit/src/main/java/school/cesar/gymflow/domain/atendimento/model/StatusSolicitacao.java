package school.cesar.gymflow.domain.atendimento.model;

/** Situacao de pedidos que a equipe aprova ou rejeita. EXPIRADA e calculada (pendente ha mais de 7 dias). */
public enum StatusSolicitacao {
    PENDENTE,
    APROVADA,
    REJEITADA,
    EXPIRADA
}
