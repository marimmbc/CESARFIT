package school.cesar.gymflow.infrastructure.evento;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.evento.BarramentoDeEventos;
import school.cesar.gymflow.domain.shared.evento.Evento;
import school.cesar.gymflow.domain.shared.evento.ManipuladorDeEvento;

/**
 * Adaptador: barramento sincrono (dentro da mesma transacao). Entrega o evento a todo manipulador
 * cujo tipo combine e registra o fato na trilha de auditoria.
 * Os manipuladores sao buscados na hora do envio (ObjectProvider) para evitar dependencia circular.
 */
@Component
public class BarramentoSincrono implements BarramentoDeEventos {

    private final ObjectProvider<ManipuladorDeEvento<?>> manipuladores;
    private final PublicadorDeEventos trilha;

    public BarramentoSincrono(ObjectProvider<ManipuladorDeEvento<?>> manipuladores, PublicadorDeEventos trilha) {
        this.manipuladores = manipuladores;
        this.trilha = trilha;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void postar(Evento evento) {
        trilha.publicar(EventoDeDominio.de(evento.getClass().getSimpleName(), evento.referencia(), evento.descricao()));
        manipuladores.orderedStream()
                .filter(m -> m.tipo().isInstance(evento))
                .forEach(m -> ((ManipuladorDeEvento) m).tratar(evento));
    }
}
