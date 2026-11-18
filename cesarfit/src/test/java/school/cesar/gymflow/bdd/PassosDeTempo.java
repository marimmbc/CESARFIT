package school.cesar.gymflow.bdd;

import java.time.Duration;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Quando;
import school.cesar.gymflow.domain.atendimento.model.SolicitacaoDeAtendimento;
import school.cesar.gymflow.domain.atendimento.repositorio.RepositorioDeSolicitacoes;

/** Steps que controlam o relogio usado pelas regras com prazo (SLA, janelas, regua de avisos). */
public class PassosDeTempo {

    @Autowired
    private RelogioDeTeste relogio;

    @Autowired
    private RepositorioDeSolicitacoes solicitacoes;

    @Before
    public void zerarRelogio() {
        relogio.zerar();
    }

    /**
     * O banco e compartilhado entre cenarios: pedidos de atendimento que ficaram aguardando
     * sao cancelados para nao "furar" a fila priorizada do proximo cenario.
     */
    @After
    public void limparFilaDeAtendimento() {
        relogio.zerar();
        solicitacoes.atendimentos().stream().filter(SolicitacaoDeAtendimento::aguardando).forEach(a -> {
            a.cancelar();
            solicitacoes.salvar(a);
        });
    }

    @Quando("se passarem {int} minutos")
    public void passaremMinutos(int minutos) {
        relogio.avancar(Duration.ofMinutes(minutos));
    }

    @Quando("se passarem {int} horas")
    public void passaremHoras(int horas) {
        relogio.avancar(Duration.ofHours(horas));
    }

    @Quando("se passarem {int} dias")
    public void passaremDias(int dias) {
        relogio.avancar(Duration.ofDays(dias));
    }

    @Dado("que o relogio marca amanha as {int}h")
    public void relogioAmanha(int hora) {
        relogio.posicionarEm(LocalDate.now().plusDays(1).atTime(hora, 0));
    }
}
