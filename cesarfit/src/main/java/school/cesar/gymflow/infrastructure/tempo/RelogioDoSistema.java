package school.cesar.gymflow.infrastructure.tempo;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import school.cesar.gymflow.domain.shared.Relogio;

/** Adaptador: relogio real do servidor. */
@Component
public class RelogioDoSistema implements Relogio {

    @Override
    public LocalDateTime agora() {
        return LocalDateTime.now();
    }
}
