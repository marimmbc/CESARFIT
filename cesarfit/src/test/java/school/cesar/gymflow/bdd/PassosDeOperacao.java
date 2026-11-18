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
import school.cesar.gymflow.application.usecase.MatricularAlunoUseCase;
import school.cesar.gymflow.application.usecase.NotificacaoDeVencimentoUseCase;
import school.cesar.gymflow.application.usecase.OcorrenciaDeEquipamentoUseCase;
import school.cesar.gymflow.application.usecase.ServicoDeConsultas;

public class PassosDeOperacao {

    @Autowired
    private ContextoDeTeste contexto;

    @Autowired
    private MatricularAlunoUseCase matricular;

    @Autowired
    private NotificacaoDeVencimentoUseCase notificacoes;

    @Autowired
    private OcorrenciaDeEquipamentoUseCase ocorrencias;

    @Autowired
    private ServicoDeConsultas consultas;

    private Respostas.OcorrenciaResumo ocorrencia;

    private void capturar(Runnable acao) {
        try {
            acao.run();
        } catch (RegraDeNegocioException e) {
            contexto.erro(e);
        }
    }

    @Dado("que o aluno tem uma matricula {string} iniciada ha {int} dias")
    public void matriculaIniciadaHa(String plano, int dias) {
        contexto.matricula(matricular.executar(new Comandos.SalvarMatricula(
                contexto.alunoId(), plano, LocalDate.now().minusDays(dias), null)));
    }

    @Quando("o sistema verificar os vencimentos")
    public void verificar() {
        notificacoes.verificar(LocalDate.now());
    }

    @Entao("o aluno deve ter {int} aviso de vencimento")
    public void avisos(int quantidade) {
        assertEquals(quantidade, notificacoes.doAluno(contexto.alunoId()).size());
    }

    @Quando("eu registrar uma ocorrencia para o equipamento {string} com a descricao {string}")
    public void registrar(String equipamento, String descricao) {
        ocorrencia = ocorrencias.registrar(new Comandos.RegistrarOcorrencia(equipamento, descricao));
    }

    @Quando("eu tentar registrar uma ocorrencia para o equipamento {string} com a descricao {string}")
    public void tentarRegistrar(String equipamento, String descricao) {
        capturar(() -> ocorrencias.registrar(new Comandos.RegistrarOcorrencia(equipamento, descricao)));
    }

    @Quando("eu colocar a ocorrencia em andamento")
    public void iniciar() {
        ocorrencia = ocorrencias.iniciar(ocorrencia.id());
    }

    @Quando("eu resolver a ocorrencia com a solucao {string}")
    public void resolver(String solucao) {
        ocorrencia = ocorrencias.resolver(ocorrencia.id(), solucao);
    }

    @Quando("eu tentar resolver a ocorrencia com a solucao {string}")
    public void tentarResolver(String solucao) {
        capturar(() -> resolver(solucao));
    }

    @Entao("a ocorrencia deve estar {string}")
    public void ocorrenciaDeveEstar(String status) {
        assertEquals(status, ocorrencia.status());
    }

    /* ---------- regua de vencimento ---------- */

    @Quando("o sistema verificar os vencimentos {int} dias antes do fim da matricula")
    public void verificarAntes(int dias) {
        notificacoes.verificar(contexto.matricula().fim().minusDays(dias));
    }

    @Quando("o sistema verificar os vencimentos {int} dias apos o fim da matricula")
    public void verificarDepois(int dias) {
        notificacoes.verificar(contexto.matricula().fim().plusDays(dias));
    }

    @Entao("o aluno deve ter recebido as etapas {string}")
    public void etapasRecebidas(String etapas) {
        List<String> esperadas = java.util.Arrays.stream(etapas.split(",")).map(String::trim).sorted().toList();
        List<String> recebidas = notificacoes.doAluno(contexto.alunoId()).stream()
                .map(Respostas.NotificacaoResumo::etapa).sorted().toList();
        assertEquals(esperadas, recebidas);
    }

    @Entao("o status salvo da matricula deve ser {string}")
    public void statusSalvo(String status) {
        assertEquals(status, consultas.matricula(contexto.matricula().id()).status());
    }

    /* ---------- ocorrencias ---------- */

    @Quando("eu registrar uma ocorrencia {string} para o equipamento {string} com a descricao {string}")
    public void registrarComSeveridade(String severidade, String equipamento, String descricao) {
        ocorrencia = ocorrencias.registrar(new Comandos.RegistrarOcorrencia(equipamento, descricao, severidade));
    }

    @Dado("que o equipamento {string} ja teve {int} ocorrencias resolvidas neste mes")
    public void ocorrenciasResolvidas(String equipamento, int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            registrar(equipamento, "Problema recorrente numero " + (i + 1));
            iniciar();
            resolver("Ajuste realizado");
        }
    }

    @Quando("a ocorrencia aguardar a peca {string}")
    public void aguardarPeca(String peca) {
        ocorrencia = ocorrencias.aguardarPeca(ocorrencia.id(), peca);
    }

    @Quando("a peca chegar e a ocorrencia for retomada")
    public void retomar() {
        ocorrencia = ocorrencias.retomar(ocorrencia.id());
    }

    @Quando("eu resolver a ocorrencia com a solucao {string} e o custo {string}")
    public void resolverComCusto(String solucao, String custo) {
        ocorrencia = ocorrencias.resolver(ocorrencia.id(), new Comandos.ResolverOcorrencia(solucao, custo));
    }

    @Quando("eu reabrir a ocorrencia com o motivo {string}")
    public void reabrir(String motivo) {
        ocorrencia = ocorrencias.reabrir(ocorrencia.id(), motivo);
    }

    @Quando("eu tentar reabrir a ocorrencia com o motivo {string}")
    public void tentarReabrir(String motivo) {
        capturar(() -> reabrir(motivo));
    }

    @Entao("o equipamento deve estar interditado")
    public void interditado() {
        assertTrue(ocorrencias.detalhar(ocorrencia.id()).interditado());
    }

    @Entao("o equipamento nao deve estar interditado")
    public void naoInterditado() {
        assertFalse(ocorrencias.detalhar(ocorrencia.id()).interditado());
    }

    @Entao("a ocorrencia deve estar atrasada")
    public void atrasada() {
        assertTrue(ocorrencias.detalhar(ocorrencia.id()).atrasada());
    }

    @Entao("a ocorrencia nao deve estar atrasada")
    public void naoAtrasada() {
        assertFalse(ocorrencias.detalhar(ocorrencia.id()).atrasada());
    }

    @Entao("a ocorrencia deve ter {int} reabertura")
    public void reaberturas(int quantidade) {
        assertEquals(quantidade, ocorrencia.reaberturas());
    }

    @Entao("a ocorrencia deve ter severidade {string}")
    public void severidade(String severidade) {
        assertEquals(severidade, ocorrencia.severidade());
    }

    @Entao("a ocorrencia deve estar marcada como reincidente")
    public void reincidente() {
        assertTrue(ocorrencia.reincidente());
    }
}
