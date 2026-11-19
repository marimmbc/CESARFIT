package school.cesar.gymflow.domain.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    void converteTextoNoFormatoBrasileiro() {
        assertEquals(new BigDecimal("1320.50"), Dinheiro.de("R$ 1.320,50").valor());
        assertEquals(new BigDecimal("120.00"), Dinheiro.de("120").valor());
    }

    @Test
    void rejeitaValorVazioOuInvalido() {
        assertThrows(RegraDeNegocioException.class, () -> Dinheiro.de(""));
        assertThrows(RegraDeNegocioException.class, () -> Dinheiro.de((String) null));
        assertThrows(RegraDeNegocioException.class, () -> Dinheiro.de("abc"));
    }

    @Test
    void somaSubtraiEMultiplica() {
        Dinheiro cem = Dinheiro.de("100,00");
        assertEquals(Dinheiro.de("150,00"), cem.somar(Dinheiro.de("50,00")));
        assertEquals(Dinheiro.de("70,00"), cem.subtrair(Dinheiro.de("30,00")));
        assertEquals(Dinheiro.de("300,00"), cem.multiplicar(3));
    }

    @Test
    void aplicaPercentualDescontoEProporcao() {
        Dinheiro valor = Dinheiro.de("120,00");
        assertEquals(Dinheiro.de("2,40"), valor.percentual(new BigDecimal("2")));
        assertEquals(Dinheiro.de("108,00"), valor.aplicarDesconto(10));
        assertEquals(Dinheiro.de("40,00"), valor.proporcao(10, 30));
        assertEquals(Dinheiro.ZERO, valor.proporcao(10, 0));
    }

    @Test
    void naoNegativoEComparacoes() {
        Dinheiro negativo = Dinheiro.de("10,00").subtrair(Dinheiro.de("25,00"));
        assertEquals(Dinheiro.ZERO, negativo.naoNegativo());
        assertTrue(Dinheiro.de("10,00").maiorQue(Dinheiro.de("9,99")));
        assertTrue(Dinheiro.de("10,00").maiorOuIgual(Dinheiro.de("10,00")));
        assertFalse(Dinheiro.ZERO.positivo());
        assertTrue(Dinheiro.ZERO.zero());
        assertEquals(Dinheiro.de("5,00"), Dinheiro.de("5,00").minimo(Dinheiro.de("8,00")));
    }

    @Test
    void formataEmReais() {
        assertEquals("R$ 120,00", Dinheiro.de("120").toString());
    }
}
