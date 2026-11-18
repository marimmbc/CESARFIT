package school.cesar.gymflow.bdd;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import school.cesar.gymflow.domain.shared.Relogio;

/** Relogio controlavel pelos cenarios: comeca hoje as 10h e os steps o avancam. */
@Primary
@Component
public class RelogioDeTeste implements Relogio {

    private Duration deslocamento = Duration.between(LocalDateTime.now(), java.time.LocalDate.now().atTime(10, 0));

    @Override
    public LocalDateTime agora() {
        return LocalDateTime.now().plus(deslocamento);
    }

    public void avancar(Duration tempo) {
        deslocamento = deslocamento.plus(tempo);
    }

    /** Posiciona o relogio em um instante exato (mantendo-o andando a partir dali). */
    public void posicionarEm(LocalDateTime instante) {
        deslocamento = Duration.between(LocalDateTime.now(), instante);
    }

    /** Cada cenario comeca hoje as 10h (academia aberta), independente da hora real em que o teste roda. */
    public void zerar() {
        posicionarEm(java.time.LocalDate.now().atTime(10, 0));
    }
}
