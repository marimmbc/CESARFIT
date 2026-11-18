package school.cesar.gymflow.domain.operacao.model;

/** ABERTA -> EM_ANDAMENTO <-> AGUARDANDO_PECA -> RESOLVIDA -> (reabertura em ate 7 dias) ABERTA. */
public enum StatusOcorrencia {
    ABERTA,
    EM_ANDAMENTO,
    AGUARDANDO_PECA,
    RESOLVIDA
}
