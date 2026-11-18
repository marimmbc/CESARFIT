package school.cesar.gymflow.domain.shared.evento;

/** Porta de saida: entrega um evento aos manipuladores interessados (em outros contextos). */
public interface BarramentoDeEventos {

    void postar(Evento evento);

    default void postarTodos(Iterable<? extends Evento> eventos) {
        for (Evento evento : eventos) {
            postar(evento);
        }
    }
}
