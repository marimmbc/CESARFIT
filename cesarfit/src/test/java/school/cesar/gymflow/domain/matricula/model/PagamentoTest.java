package school.cesar.gymflow.domain.matricula.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

class PagamentoTest {

    private static final String MOTIVO = "Cobranca feita por engano";

    private final LocalDate hoje = LocalDate.now();

    private Pagamento pagamentoDe(String valor) {
        return new Pagamento(Dinheiro.de(valor), hoje, "PIX");
    }

    @Test
    void estornoIntegralRegistraMotivoECategoria() {
        Pagamento estornado = pagamentoDe("120,00").estornar(Dinheiro.de("120,00"), MotivoDeEstorno.OUTRO, MOTIVO, hoje);
        assertTrue(estornado.estornado());
        assertEquals(MOTIVO, estornado.motivoDoEstorno());
        assertEquals(MotivoDeEstorno.OUTRO, estornado.categoriaDoEstorno());
        assertEquals(hoje, estornado.dataDoEstorno());
        assertEquals(Dinheiro.ZERO, estornado.saldoEstornavel());
    }

    @Test
    void estornoExigeMotivoComDezCaracteres() {
        assertThrows(RegraDeNegocioException.class,
                () -> pagamentoDe("120,00").estornar(Dinheiro.de("120,00"), MotivoDeEstorno.OUTRO, "curto", hoje));
        assertThrows(RegraDeNegocioException.class,
                () -> pagamentoDe("120,00").estornar(Dinheiro.de("120,00"), MotivoDeEstorno.OUTRO, null, hoje));
    }

    @Test
    void estornoDuplicadoERejeitado() {
        Pagamento estornado = pagamentoDe("120,00").estornar(Dinheiro.de("120,00"), MotivoDeEstorno.OUTRO, MOTIVO, hoje);
        assertThrows(RegraDeNegocioException.class,
                () -> estornado.estornar(Dinheiro.de("1,00"), MotivoDeEstorno.OUTRO, MOTIVO, hoje));
    }

    @Test
    void estornoParcialMantemSaldoERejeitaExcesso() {
        Pagamento parcial = pagamentoDe("120,00").estornar(Dinheiro.de("20,00"), MotivoDeEstorno.OUTRO, MOTIVO, hoje);
        assertFalse(parcial.estornado());
        assertTrue(parcial.parcialmenteEstornado());
        assertEquals(Dinheiro.de("100,00"), parcial.saldoEstornavel());
        assertThrows(RegraDeNegocioException.class,
                () -> parcial.estornar(Dinheiro.de("100,01"), MotivoDeEstorno.OUTRO, MOTIVO, hoje));
        assertTrue(parcial.estornar(Dinheiro.de("100,00"), MotivoDeEstorno.OUTRO, MOTIVO, hoje).estornado());
    }

    @Test
    void estornoRejeitaValorZero() {
        assertThrows(RegraDeNegocioException.class,
                () -> pagamentoDe("120,00").estornar(Dinheiro.ZERO, MotivoDeEstorno.OUTRO, MOTIVO, hoje));
    }

    @Test
    void pagamentoEmAtrasoCobraMultaDe2PorCentoEJurosDiarios() {
        // Matricula mensal que venceu ha 10 dias
        Matricula m = Matricula.abrir(1L, Modalidade.MENSAL, Dinheiro.de("120,00"), hoje.minusMonths(1).minusDays(10));
        assertEquals(10, m.diasDeAtraso(hoje));
        assertEquals(Dinheiro.de("2,40"), m.multaEm(hoje));
        assertEquals(Dinheiro.de("0,40"), m.jurosEm(hoje));

        Pagamento pago = m.registrarPagamento(Dinheiro.de("122,80"), FormaDePagamento.PIX, 1, hoje);
        assertEquals(Dinheiro.de("122,80"), pago.valor());
        assertTrue(pago.comEncargos());
        assertEquals(StatusMatricula.ATIVA, m.status());
    }

    @Test
    void pagamentoEmDiaNaoTemEncargos() {
        Matricula m = Matricula.abrir(1L, Modalidade.MENSAL, Dinheiro.de("120,00"), hoje);
        Pagamento pago = m.registrarPagamento(Dinheiro.de("120,00"), FormaDePagamento.PIX, 1, hoje);
        assertFalse(pago.comEncargos());
    }

    @Test
    void parcelamentoSoNoCartaoDeCreditoESoNoPlanoAnual() {
        Matricula anual = Matricula.abrir(1L, Modalidade.ANUAL, Dinheiro.de("1320,00"), hoje);
        Pagamento parcelado = anual.registrarPagamento(Dinheiro.de("1320,00"), FormaDePagamento.CARTAO_CREDITO, 12, hoje);
        assertEquals(12, parcelado.parcelas());

        Matricula outraAnual = Matricula.abrir(1L, Modalidade.ANUAL, Dinheiro.de("1320,00"), hoje);
        assertThrows(RegraDeNegocioException.class,
                () -> outraAnual.registrarPagamento(Dinheiro.de("1320,00"), FormaDePagamento.PIX, 3, hoje));
        assertThrows(RegraDeNegocioException.class,
                () -> outraAnual.registrarPagamento(Dinheiro.de("1320,00"), FormaDePagamento.CARTAO_CREDITO, 13, hoje));

        Matricula mensal = Matricula.abrir(1L, Modalidade.MENSAL, Dinheiro.de("120,00"), hoje);
        assertThrows(RegraDeNegocioException.class,
                () -> mensal.registrarPagamento(Dinheiro.de("120,00"), FormaDePagamento.CARTAO_CREDITO, 2, hoje));
    }

    @Test
    void parcelaMinimaEUmaParcela() {
        assertEquals(1, new Pagamento(Dinheiro.de("10,00"), hoje, "PIX", false, null, null, null, null,
                null, null, 0, null).parcelas());
    }
}
