package school.cesar.gymflow.domain.shared.evento;

import java.util.ArrayList;
import java.util.List;

/** Base das raizes de agregado que registram eventos ao mudar de estado. */
public abstract class RaizDeAgregado {

    private final transient List<Evento> eventos = new ArrayList<>();

    protected void registrar(Evento evento) {
        eventos.add(evento);
    }

    /** Devolve e limpa os eventos registrados (o caso de uso posta no barramento depois de salvar). */
    public List<Evento> retirarEventos() {
        List<Evento> copia = List.copyOf(eventos);
        eventos.clear();
        return copia;
    }
}
