package school.cesar.gymflow.domain.treino.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import school.cesar.gymflow.domain.matricula.model.Objetivo;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

class FichaDeTreinoTest {

    private FichaDeTreino novaFicha() {
        return new FichaDeTreino(null, 1L, Objetivo.HIPERTROFIA, LocalDate.now(),
                LocalDate.now().plusMonths(1), List.of());
    }

    @Test
    void fichaNovaNasceAtiva() {
        FichaDeTreino ficha = novaFicha();
        assertEquals(StatusFicha.ATIVA, ficha.status());
        assertTrue(ficha.ativa());
    }

    @Test
    void arquivarTiraAFichaDeUso() {
        FichaDeTreino ficha = novaFicha();
        ficha.arquivar();
        assertEquals(StatusFicha.ARQUIVADA, ficha.status());
        assertFalse(ficha.ativa());
    }

    @Test
    void naoArquivaFichaJaArquivada() {
        FichaDeTreino ficha = novaFicha();
        ficha.arquivar();
        assertThrows(RegraDeNegocioException.class, ficha::arquivar);
    }
}
