package school.cesar.gymflow.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.After;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.application.usecase.AtualizarMatriculaUseCase;
import school.cesar.gymflow.application.usecase.CancelarMatriculaUseCase;
import school.cesar.gymflow.application.usecase.ContestacaoDeCobrancaUseCase;
import school.cesar.gymflow.application.usecase.EstornarPagamentoUseCase;
import school.cesar.gymflow.domain.shared.Dinheiro;

public class PassosFinanceiros {

    @Autowired
    private ContextoDeTeste contexto;

    @Autowired
    private CancelarMatriculaUseCase cancelar;

    @Autowired
    private AtualizarMatriculaUseCase atualizar;

    @Autowired
    private EstornarPagamentoUseCase estornar;

    @Autowired
    private ContestacaoDeCobrancaUseCase contestacoes;

    @Autowired
    private school.cesar.gymflow.application.usecase.ServicoDeConsultas consultas;

    private Respostas.ContestacaoResumo contestacao;

    private void capturar(Runnable acao) {
        try {
            acao.run();
        } catch (RegraDeNegocioException e) {
            contexto.erro(e);
        }
    }

    @Quando("eu cancelar a matricula com a justificativa {string}")
    public void cancelar(String justificativa) {
        contexto.matricula(cancelar.executar(contexto.matricula().id(), justificativa));
    }

    @Quando("eu tentar cancelar a matricula com a justificativa {string}")
    public void tentarCancelar(String justificativa) {
        capturar(() -> cancelar(justificativa));
    }

    @Quando("eu tentar alterar a situacao da matricula para {string}")
    public void tentarAlterarSituacao(String status) {
        var atual = contexto.matricula();
        capturar(() -> atualizar.executar(atual.id(),
                new Comandos.SalvarMatricula(atual.alunoId(), atual.plano(), atual.inicio(), status)));
    }

    @Entao("a justificativa e a data da solicitacao devem estar registradas")
    public void cancelamentoRegistrado() {
        assertEquals("Mudanca de cidade por trabalho", contexto.matricula().justificativaDoCancelamento());
        assertEquals(LocalDate.now(), contexto.matricula().dataDaSolicitacaoDeCancelamento());
    }

    @Entao("a taxa de cancelamento deve ser {string}")
    public void taxaDeCancelamento(String valor) {
        assertEquals(valor, contexto.matricula().taxaDeCancelamento());
    }

    @Entao("nao deve haver taxa de cancelamento")
    public void semTaxa() {
        assertEquals(Dinheiro.ZERO.toString(), contexto.matricula().taxaDeCancelamento());
    }

    @Quando("eu estornar o pagamento {int} com o motivo {string}")
    public void estornar(int indice, String motivo) {
        contexto.matricula(estornar.executar(contexto.matricula().id(), indice, motivo));
    }

    @Quando("eu tentar estornar o pagamento {int} com o motivo {string}")
    public void tentarEstornar(int indice, String motivo) {
        capturar(() -> estornar(indice, motivo));
    }

    @Entao("o pagamento {int} deve estar estornado")
    public void estornado(int indice) {
        assertTrue(recarregar().pagamentos().get(indice).estornado());
    }

    @Entao("o pagamento {int} nao deve estar estornado")
    public void naoEstornado(int indice) {
        assertFalse(recarregar().pagamentos().get(indice).estornado());
    }

    @E("o total pago da matricula deve ser zero")
    public void totalZero() {
        assertEquals(Dinheiro.ZERO.toString(), contexto.matricula().totalPago());
    }

    @Quando("o aluno contestar o pagamento {int} com o motivo {string}")
    public void contestar(int indice, String motivo) {
        contestacao = contestacoes.abrir(contexto.alunoId(),
                new Comandos.AbrirContestacao(contexto.matricula().id(), indice, motivo));
    }

    @Quando("o aluno tentar contestar o pagamento {int} com o motivo {string}")
    public void tentarContestar(int indice, String motivo) {
        capturar(() -> contestar(indice, motivo));
    }

    @Quando("a equipe analisar a contestacao como {string} com o parecer {string}")
    public void analisar(String resultado, String parecer) {
        contestacao = contestacoes.analisar(contestacao.id(),
                new Comandos.AnalisarContestacao("procedente".equalsIgnoreCase(resultado), parecer));
    }

    @Entao("a contestacao deve estar {string}")
    public void contestacaoDeveEstar(String status) {
        assertNotNull(contestacao);
        assertEquals(status, contestacao.status());
    }

    /* ---------- estorno com categoria ---------- */

    @Quando("eu estornar {string} do pagamento {int} na categoria {string} com o motivo {string}")
    public void estornarComCategoria(String valor, int indice, String categoria, String motivo) {
        contexto.matricula(estornar.executar(contexto.matricula().id(), indice,
                new Comandos.EstornarPagamento(valor, categoria, motivo)));
    }

    @Quando("eu tentar estornar {string} do pagamento {int} na categoria {string} com o motivo {string}")
    public void tentarEstornarComCategoria(String valor, int indice, String categoria, String motivo) {
        capturar(() -> estornarComCategoria(valor, indice, categoria, motivo));
    }

    @Entao("o total pago da matricula deve ser {string}")
    public void totalPago(String valor) {
        assertEquals(valor, contexto.matricula().totalPago());
    }

    @Entao("o total pago salvo da matricula deve ser {string}")
    public void totalPagoSalvo(String valor) {
        assertEquals(valor, recarregar().totalPago());
    }

    @Entao("o pagamento {int} deve estar parcialmente estornado")
    public void parcialmenteEstornado(int indice) {
        assertTrue(recarregar().pagamentos().get(indice).parcialmenteEstornado());
    }

    /* ---------- contestacao: analista, pedido de informacoes e recurso ---------- */

    @Quando("o analista {string} decidir a contestacao como {string} com o parecer {string}")
    public void decidir(String analista, String resultado, String parecer) {
        contestacao = contestacoes.analisar(contestacao.id(),
                new Comandos.AnalisarContestacao(null, resultado, null, parecer), analista);
    }

    @Quando("o analista {string} tentar decidir a contestacao como {string} com o parecer {string}")
    public void tentarDecidir(String analista, String resultado, String parecer) {
        capturar(() -> decidir(analista, resultado, parecer));
    }

    @Quando("o analista {string} decidir a contestacao como {string} estornando {string} com o parecer {string}")
    public void decidirParcial(String analista, String resultado, String valor, String parecer) {
        contestacao = contestacoes.analisar(contestacao.id(),
                new Comandos.AnalisarContestacao(null, resultado, valor, parecer), analista);
    }

    @Quando("a equipe pedir informacoes com a pergunta {string}")
    public void pedirInformacoes(String pergunta) {
        contestacao = contestacoes.pedirInformacoes(contestacao.id(), pergunta);
    }

    @Quando("o aluno responder a contestacao com {string}")
    public void responder(String resposta) {
        contestacao = contestacoes.responder(contexto.alunoId(), contestacao.id(), resposta);
    }

    @Quando("o aluno recorrer com a justificativa {string}")
    public void recorrer(String justificativa) {
        contestacao = contestacoes.recorrer(contexto.alunoId(), contestacao.id(), justificativa);
    }

    @Quando("o aluno tentar recorrer com a justificativa {string}")
    public void tentarRecorrer(String justificativa) {
        capturar(() -> recorrer(justificativa));
    }

    @Entao("a contestacao deve estar atrasada")
    public void contestacaoAtrasada() {
        Long id = contestacao.id();
        assertTrue(contestacoes.todas().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow().atrasada());
    }

    /** Le a matricula de novo do banco (a contestacao procedente estorna fora do contexto do teste). */
    private Respostas.MatriculaResumo recarregar() {
        return consultas.matricula(contexto.matricula().id());
    }
}
