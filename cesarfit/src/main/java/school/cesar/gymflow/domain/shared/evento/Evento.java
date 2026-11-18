package school.cesar.gymflow.domain.shared.evento;

/**
 * Evento de dominio tipado: algo que aconteceu em um agregado e que outros contextos podem reagir.
 * Cada evento e um objeto de valor imutavel (record) com os dados do fato.
 */
public interface Evento {

    /** Identificador do agregado de origem (para a trilha de auditoria). */
    String referencia();

    /** Descricao legivel do fato (para a trilha de auditoria). */
    String descricao();
}
