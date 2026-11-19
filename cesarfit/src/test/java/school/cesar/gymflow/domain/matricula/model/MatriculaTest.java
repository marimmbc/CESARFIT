package school.cesar.gymflow.domain.matricula.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

class MatriculaTest {

    private static final Dinheiro MENSAL = Dinheiro.de("120,00");
    private static final Dinheiro ANUAL = Dinheiro.de("1320,00");
    private static final String JUSTIFICATIVA = "Mudanca de cidade a trabalho";

    private final LocalDate hoje = LocalDate.now();

    @Test
    void abrirCriaMatriculaAtivaComVigenciaDoPlano() {
        Matricula mensal = Matricula.abrir(1L, Modalidade.MENSAL, MENSAL, hoje);
        assertEquals(StatusMatricula.ATIVA, mensal.status());
        assertEquals(hoje.plusMonths(1), mensal.fim());

        Matricula anual = Matricula.abrir(1L, Modalidade.ANUAL, ANUAL, hoje);
        assertEquals(hoje.plusMonths(12), anual.fim());
    }

    @Test
    void abrirRejeitaDadosInvalidos() {
        assertThrows(RegraDeNegocioException.class, () -> Matricula.abrir(null, Modalidade.MENSAL, MENSAL, hoje));
        assertThrows(RegraDeNegocioException.class, () -> Matricula.abrir(1L, null, MENSAL, hoje));
        assertThrows(RegraDeNegocioException.class, () -> Matricula.abrir(1L, Modalidade.MENSAL, Dinheiro.ZERO, hoje));
        assertThrows(RegraDeNegocioException.class, () -> Matricula.abrir(1L, Modalidade.MENSAL, MENSAL, null));
        assertThrows(RegraDeNegocioException.class,
                () -> Matricula.abrir(1L, Modalidade.MENSAL, MENSAL, hoje.plusYears(2)));
    }

    @Test
    void renovarEstendeAPartirDoFimQuandoAindaVigente() {
        Matricula m = Matricula.abrir(1L, Modalidade.MENSAL, MENSAL, hoje);
        LocalDate fimAnterior = m.fim();
        m.renovar();
        assertEquals(fimAnterior.plusMonths(1), m.fim());
        assertEquals(StatusMatricula.ATIVA, m.status());
    }

    @Test
    void renovarMatriculaVencidaContaAPartirDeHoje() {
        Matricula m = Matricula.abrir(1L, Modalidade.MENSAL, MENSAL, hoje.minusMonths(3));
        m.atualizarVigencia(hoje);
        m.renovar();
        assertEquals(hoje.plusMonths(1), m.fim());
        assertEquals(StatusMatricula.ATIVA, m.status());
    }

    @Test
    void renovarMatriculaCanceladaNaoEPermitido() {
        Matricula m = Matricula.abrir(1L, Modalidade.MENSAL, MENSAL, hoje);
        m.cancelar(JUSTIFICATIVA, hoje);
        assertThrows(RegraDeNegocioException.class, m::renovar);
    }

    @Test
    void cancelarExigeJustificativaComDezCaracteres() {
        Matricula m = Matricula.abrir(1L, Modalidade.MENSAL, MENSAL, hoje);
        assertThrows(RegraDeNegocioException.class, () -> m.cancelar("curta", hoje));
        assertEquals(StatusMatricula.ATIVA, m.status());
    }

    @Test
    void cancelarRejeitaDataFuturaECancelamentoDuplicado() {
        Matricula m = Matricula.abrir(1L, Modalidade.MENSAL, MENSAL, hoje);
        assertThrows(RegraDeNegocioException.class, () -> m.cancelar(JUSTIFICATIVA, hoje.plusDays(1)));
        m.cancelar(JUSTIFICATIVA, hoje);
        assertEquals(StatusMatricula.CANCELADA, m.status());
        assertEquals(hoje, m.dataDaSolicitacaoDeCancelamento());
        assertThrows(RegraDeNegocioException.class, () -> m.cancelar(JUSTIFICATIVA, hoje));
    }

    @Test
    void taxaDeUmaMensalidadeNoPlanoMensalComMenosDe30Dias() {
        Matricula m = Matricula.abrir(1L, Modalidade.MENSAL, MENSAL, hoje);
        assertEquals(Dinheiro.de("120,00"), m.taxaPara(m.fim().minusDays(29)));
    }

    @Test
    void taxaDeUmaMensalidadeNoPlanoAnualComMenosDe30Dias() {
        Matricula m = Matricula.abrir(1L, Modalidade.ANUAL, ANUAL, hoje);
        assertEquals(Dinheiro.de("110,00"), m.taxaPara(m.fim().minusDays(10)));
    }

    @Test
    void semTaxaComTrintaDiasOuMaisDeAntecedencia() {
        Matricula m = Matricula.abrir(1L, Modalidade.ANUAL, ANUAL, hoje);
        assertEquals(Dinheiro.ZERO, m.taxaPara(m.fim().minusDays(30)));
        assertEquals(Dinheiro.ZERO, m.taxaPara(hoje));
    }

    @Test
    void cancelamentoRegistraATaxaCalculada() {
        Matricula m = Matricula.abrir(1L, Modalidade.MENSAL, MENSAL, hoje.minusDays(20));
        m.cancelar(JUSTIFICATIVA, hoje);
        assertEquals(Dinheiro.de("120,00"), m.taxaDeCancelamento());
    }
}
