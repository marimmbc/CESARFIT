package school.cesar.gymflow.domain.shared;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Porta de saida: fonte da data/hora atual.
 * As regras com prazo (SLA, janelas de estorno, regua de avisos) recebem o "agora" por aqui,
 * o que deixa o dominio deterministico e testavel (o BDD avanca o relogio).
 */
public interface Relogio {

    LocalDateTime agora();

    default LocalDate hoje() {
        return agora().toLocalDate();
    }
}
