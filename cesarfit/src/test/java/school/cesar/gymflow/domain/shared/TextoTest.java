package school.cesar.gymflow.domain.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextoTest {

    @Test
    void obrigatorioRemoveEspacosDasPontas() {
        assertEquals("Ana", Texto.obrigatorio("  Ana  ", 3, 10, "Nome"));
    }

    @Test
    void obrigatorioAceitaOsLimitesExatos() {
        assertEquals("abc", Texto.obrigatorio("abc", 3, 5, "Campo"));
        assertEquals("abcde", Texto.obrigatorio("abcde", 3, 5, "Campo"));
    }

    @Test
    void obrigatorioRejeitaAbaixoDoMinimo() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> Texto.obrigatorio("ab", 3, 5, "Campo"));
        assertTrue(erro.getMessage().contains("pelo menos 3"));
    }

    @Test
    void obrigatorioRejeitaAcimaDoMaximo() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> Texto.obrigatorio("abcdef", 3, 5, "Campo"));
        assertTrue(erro.getMessage().contains("no maximo 5"));
    }

    @Test
    void obrigatorioRejeitaNuloOuEmBranco() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> Texto.obrigatorio("   ", 1, 5, "Campo"));
        assertEquals("Campo e obrigatorio", erro.getMessage());
        assertThrows(RegraDeNegocioException.class, () -> Texto.obrigatorio(null, 1, 5, "Campo"));
    }

    @Test
    void opcionalDevolveNuloQuandoVazio() {
        assertNull(Texto.opcional("  ", 5, "Campo"));
        assertNull(Texto.opcional(null, 5, "Campo"));
        assertEquals("ok", Texto.opcional(" ok ", 5, "Campo"));
    }
}
