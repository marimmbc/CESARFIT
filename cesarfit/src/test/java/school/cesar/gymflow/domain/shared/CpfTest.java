package school.cesar.gymflow.domain.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CpfTest {

    @Test
    void aceitaCpfComOnzeDigitosENormalizaAPontuacao() {
        Cpf cpf = Cpf.de("111.222.333-44");
        assertEquals("11122233344", cpf.numero());
        assertEquals("111.222.333-44", cpf.formatado());
    }

    @Test
    void rejeitaCpfNaoInformado() {
        assertThrows(RegraDeNegocioException.class, () -> Cpf.de(null));
    }

    @Test
    void rejeitaCpfComQuantidadeErradaDeDigitos() {
        assertThrows(RegraDeNegocioException.class, () -> Cpf.de("1234567890"));
        assertThrows(RegraDeNegocioException.class, () -> Cpf.de("123456789012"));
    }

    @Test
    void rejeitaSequenciaRepetida() {
        assertThrows(RegraDeNegocioException.class, () -> Cpf.de("11111111111"));
        assertThrows(RegraDeNegocioException.class, () -> Cpf.de("000.000.000-00"));
    }

    @Test
    void cpfsComOsMesmosDigitosSaoIguais() {
        assertEquals(Cpf.de("55566677788"), Cpf.de("555.666.777-88"));
    }
}
