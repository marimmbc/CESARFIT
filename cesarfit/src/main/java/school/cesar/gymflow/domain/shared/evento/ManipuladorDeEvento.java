package school.cesar.gymflow.domain.shared.evento;

/** Reacao de um contexto a um evento de outro contexto. */
public interface ManipuladorDeEvento<E extends Evento> {

    Class<E> tipo();

    void tratar(E evento);
}
