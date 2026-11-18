package school.cesar.gymflow.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import school.cesar.gymflow.application.usecase.AtendimentoDoInstrutorUseCase;
import school.cesar.gymflow.application.usecase.AvaliacaoFisicaUseCase;
import school.cesar.gymflow.application.usecase.CatalogoDeExerciciosUseCase;
import school.cesar.gymflow.application.usecase.FeedbackPosTreinoUseCase;
import school.cesar.gymflow.application.usecase.ServicoDeConsultas;
import school.cesar.gymflow.application.usecase.SubstituicaoDeExercicioUseCase;

public class PassosDeSolicitacoes {

    @Autowired
    private ContextoDeTeste contexto;

    @Autowired
    private AvaliacaoFisicaUseCase avaliacoes;

    @Autowired
    private SubstituicaoDeExercicioUseCase substituicoes;

    @Autowired
    private AtendimentoDoInstrutorUseCase atendimentos;

    @Autowired
    private FeedbackPosTreinoUseCase feedbacks;

    @Autowired
    private CatalogoDeExerciciosUseCase catalogo;

    @Autowired
    private ServicoDeConsultas consultas;

    private Respostas.AvaliacaoResumo avaliacao;
    private Respostas.SubstituicaoResumo substituicao;
    private Respostas.AtendimentoResumo atendimento;

    private void capturar(Runnable acao) {
        try {
            acao.run();
        } catch (RegraDeNegocioException e) {
            contexto.erro(e);
        }
    }

    @Autowired
    private RelogioDeTeste relogio;

    @Autowired
    private PassosComuns comuns;

    @Autowired
    private school.cesar.gymflow.application.usecase.MatricularAlunoUseCase matricular;

    @Autowired
    private school.cesar.gymflow.application.usecase.GerarFichaDeTreinoUseCase gerarFicha;

    @Autowired
    private school.cesar.gymflow.application.usecase.ConcluirItemDeTreinoUseCase concluirItem;

    @Autowired
    private school.cesar.gymflow.domain.treino.repositorio.RepositorioDeFeedbacks repositorioDeFeedbacks;

    private final java.util.Map<String, Long> atendimentoPorAluno = new java.util.HashMap<>();
    private final java.util.Map<String, Long> alunoPorNome = new java.util.HashMap<>();
    private Respostas.FeedbackResumo feedback;

    /** Datas relativas ao relogio dos cenarios (que pode ter sido avancado). */
    private java.time.LocalDateTime daqui(int dias, int hora) {
        return daqui(dias, hora, 0);
    }

    private java.time.LocalDateTime daqui(int dias, int hora, int minuto) {
        return relogio.hoje().plusDays(dias).atTime(hora, minuto);
    }

    /** Cadastra, matricula e gera a ficha de um aluno extra do cenario; ele passa a ser o aluno do contexto. */
    private Long alunoComFicha(String nome, String cpf, String objetivo) {
        Long alunoId = comuns.cadastrar(nome, cpf, school.cesar.gymflow.domain.matricula.model.Objetivo.valueOf(objetivo));
        contexto.alunoId(alunoId);
        contexto.matricula(matricular.executar(new Comandos.SalvarMatricula(alunoId, "MENSAL", LocalDate.now(), null)));
        contexto.ficha(gerarFicha.executar(alunoId));
        alunoPorNome.put(nome, alunoId);
        return alunoId;
    }

    /* ---------- avaliacao fisica ---------- */

    @Quando("o aluno solicitar avaliacao fisica para daqui a {int} dias as {int}h")
    public void solicitarAvaliacao(int dias, int hora) {
        avaliacao = avaliacoes.solicitar(contexto.alunoId(), new Comandos.SolicitarAvaliacao(daqui(dias, hora)));
    }

    @Quando("o aluno tentar solicitar avaliacao fisica para daqui a {int} dias as {int}h")
    public void tentarSolicitarAvaliacao(int dias, int hora) {
        capturar(() -> solicitarAvaliacao(dias, hora));
    }

    @Quando("o aluno tentar solicitar avaliacao fisica para daqui a {int} dias as {int}:{int}")
    public void tentarSolicitarHorarioQuebrado(int dias, int hora, int minuto) {
        capturar(() -> avaliacao = avaliacoes.solicitar(contexto.alunoId(), new Comandos.SolicitarAvaliacao(
                daqui(dias, hora, minuto))));
    }

    @Quando("a equipe propor um novo horario para daqui a {int} dias as {int}h")
    public void proporHorario(int dias, int hora) {
        avaliacao = avaliacoes.proporHorario(avaliacao.id(),
                new Comandos.ResponderAvaliacao(daqui(dias, hora), "Agenda do avaliador cheia"));
    }

    @Quando("o aluno aceitar a proposta")
    public void aceitar() {
        avaliacao = avaliacoes.aceitarProposta(contexto.alunoId(), avaliacao.id());
    }

    @Entao("a avaliacao deve estar {string}")
    public void avaliacaoDeveEstar(String status) {
        assertEquals(status, avaliacao.status());
        assertNotNull(avaliacao.dataAgendada());
    }

    /* ---------- substituicao ---------- */

    @Quando("o aluno pedir a substituicao do exercicio na posicao {int} com o motivo {string}")
    public void pedirSubstituicao(int posicao, String motivo) {
        substituicao = substituicoes.solicitar(contexto.alunoId(),
                new Comandos.SolicitarSubstituicao(contexto.ficha().id(), posicao, motivo));
    }

    private Long exercicio(String nome) {
        return catalogo.listar().stream().filter(e -> e.nome().equalsIgnoreCase(nome)).findFirst()
                .orElseThrow(() -> new AssertionError("Exercicio nao esta no catalogo: " + nome)).id();
    }

    @Quando("a equipe aprovar a substituicao pelo exercicio {string}")
    public void aprovar(String nome) {
        substituicao = substituicoes.aprovar(substituicao.id(), new Comandos.AprovarSubstituicao(exercicio(nome), null));
    }

    @Quando("a equipe tentar aprovar a substituicao pelo exercicio {string}")
    public void tentarAprovar(String nome) {
        capturar(() -> aprovar(nome));
    }

    @Quando("a equipe rejeitar a substituicao com a justificativa {string}")
    public void rejeitar(String justificativa) {
        substituicao = substituicoes.rejeitar(substituicao.id(), justificativa);
    }

    @Entao("a substituicao deve estar {string}")
    public void substituicaoDeveEstar(String status) {
        assertEquals(status, substituicao.status());
    }

    @Entao("o exercicio na posicao {int} da ficha deve ser {string}")
    public void exercicioNaPosicao(int posicao, String nome) {
        var ficha = consultas.fichasDoAluno(contexto.alunoId()).get(0);
        assertEquals(nome, ficha.itens().get(posicao).exercicio());
    }

    /* ---------- atendimento ---------- */

    @Quando("o aluno pedir atendimento do instrutor com o motivo {string}")
    public void pedirAtendimento(String motivo) {
        atendimento = atendimentos.abrir(contexto.alunoId(), new Comandos.AbrirAtendimento(null, motivo));
    }

    @Quando("o instrutor {string} assumir o atendimento")
    public void assumir(String instrutor) {
        atendimento = atendimentos.assumir(atendimento.id(), instrutor);
    }

    @Quando("o instrutor concluir o atendimento com a orientacao {string}")
    public void concluir(String orientacao) {
        atendimento = atendimentos.concluir(atendimento.id(), orientacao);
    }

    @Quando("o instrutor tentar concluir o atendimento com a orientacao {string}")
    public void tentarConcluir(String orientacao) {
        capturar(() -> concluir(orientacao));
    }

    @Entao("o atendimento deve estar {string}")
    public void atendimentoDeveEstar(String status) {
        assertEquals(status, atendimento.status());
    }

    /* ---------- feedback ---------- */

    @Quando("o aluno registrar o feedback com esforco {int} e a observacao {string}")
    public void registrarFeedback(int esforco, String observacao) {
        feedbacks.registrar(contexto.alunoId(), new Comandos.RegistrarFeedback(esforco, observacao));
    }

    @Quando("o aluno tentar registrar o feedback com esforco {int} e a observacao {string}")
    public void tentarRegistrarFeedback(int esforco, String observacao) {
        capturar(() -> registrarFeedback(esforco, observacao));
    }

    @Entao("o aluno deve ter {int} feedback registrado")
    public void feedbacksRegistrados(int quantidade) {
        assertEquals(quantidade, feedbacks.doAluno(contexto.alunoId()).size());
    }

    /* =========================================================
       Regras avancadas
       ========================================================= */

    /* ---------- avaliacao fisica ---------- */

    @Quando("o aluno solicitar avaliacao fisica para daqui a {int} dias as {int}:{int}")
    public void solicitarComMinutos(int dias, int hora, int minuto) {
        avaliacao = avaliacoes.solicitar(contexto.alunoId(), new Comandos.SolicitarAvaliacao(daqui(dias, hora, minuto)));
    }

    @Quando("outro aluno matriculado chamado {string} com CPF {string} solicitar avaliacao fisica para daqui a {int} dias as {int}:{int}")
    public void outroAlunoSolicita(String nome, String cpf, int dias, int hora, int minuto) {
        Long alunoId = comuns.cadastrar(nome, cpf, school.cesar.gymflow.domain.matricula.model.Objetivo.CONDICIONAMENTO);
        contexto.alunoId(alunoId);
        matricular.executar(new Comandos.SalvarMatricula(alunoId, "MENSAL", LocalDate.now(), null));
        solicitarComMinutos(dias, hora, minuto);
    }

    @Quando("a equipe confirmar a avaliacao")
    public void confirmarAvaliacao() {
        avaliacao = avaliacoes.confirmar(avaliacao.id(), "Venha com roupa leve");
    }

    @Quando("a equipe tentar confirmar a avaliacao")
    public void tentarConfirmarAvaliacao() {
        capturar(this::confirmarAvaliacao);
    }

    @Quando("o aluno recusar a proposta")
    public void recusarProposta() {
        avaliacao = avaliacoes.recusarProposta(contexto.alunoId(), avaliacao.id());
    }

    @Quando("a equipe tentar propor um novo horario para daqui a {int} dias as {int}h")
    public void tentarProporHorario(int dias, int hora) {
        capturar(() -> proporHorario(dias, hora));
    }

    @Quando("a equipe registrar o resultado com peso {double} kg e altura {double} m")
    public void registrarResultado(double peso, double altura) {
        avaliacao = avaliacoes.registrarResultado(avaliacao.id(),
                new Comandos.RegistrarResultadoDeAvaliacao(peso, altura, null));
    }

    @Quando("a equipe tentar registrar o resultado com peso {double} kg e altura {double} m")
    public void tentarRegistrarResultado(double peso, double altura) {
        capturar(() -> registrarResultado(peso, altura));
    }

    @Quando("chegar o horario da avaliacao")
    public void chegarHorario() {
        relogio.posicionarEm(avaliacao.dataAgendada().plusMinutes(5));
    }

    @Quando("faltar {int} hora para a avaliacao")
    public void faltarHoras(int horas) {
        relogio.posicionarEm(avaliacao.dataAgendada().minusHours(horas));
    }

    @Quando("o aluno tentar cancelar a avaliacao")
    public void tentarCancelarAvaliacao() {
        capturar(() -> avaliacao = avaliacoes.cancelar(contexto.alunoId(), avaliacao.id()));
    }

    @Dado("o aluno faltou a uma avaliacao marcada para daqui a {int} dias as {int}h")
    public void faltou(int dias, int hora) {
        faltou(dias, hora, 0);
    }

    @Dado("o aluno faltou a uma avaliacao marcada para daqui a {int} dias as {int}:{int}")
    public void faltou(int dias, int hora, int minuto) {
        solicitarComMinutos(dias, hora, minuto);
        confirmarAvaliacao();
        relogio.posicionarEm(avaliacao.dataAgendada().plusMinutes(20));
        avaliacao = avaliacoes.registrarFalta(avaliacao.id());
    }

    @Entao("o IMC deve ser {string} com a classificacao {string}")
    public void imc(String imc, String classificacao) {
        assertEquals(imc, avaliacao.imc().toPlainString());
        assertEquals(classificacao, avaliacao.classificacaoImc());
    }

    /* ---------- substituicao ---------- */

    @Quando("o aluno pedir a substituicao do exercicio na posicao {int} por {string} com o motivo {string}")
    public void pedirComCategoria(int posicao, String categoria, String motivo) {
        substituicao = substituicoes.solicitar(contexto.alunoId(),
                new Comandos.SolicitarSubstituicao(contexto.ficha().id(), posicao, motivo, categoria));
    }

    @Quando("o aluno tentar pedir a substituicao do exercicio na posicao {int} por {string} com o motivo {string}")
    public void tentarPedirComCategoria(int posicao, String categoria, String motivo) {
        capturar(() -> pedirComCategoria(posicao, categoria, motivo));
    }

    @Quando("outro aluno com ficha chamado {string} com CPF {string} pedir a substituicao da posicao {int} por {string} com o motivo {string}")
    public void outroAlunoPedeTroca(String nome, String cpf, int posicao, String categoria, String motivo) {
        alunoComFicha(nome, cpf, "HIPERTROFIA");
        pedirComCategoria(posicao, categoria, motivo);
    }

    @Entao("as sugestoes devem incluir {string}")
    public void sugestoesIncluem(String nome) {
        assertTrue(substituicao.sugestoes().contains(nome), "Sugestoes: " + substituicao.sugestoes());
    }

    @Entao("as sugestoes nao devem incluir {string}")
    public void sugestoesNaoIncluem(String nome) {
        assertFalse(substituicao.sugestoes().contains(nome), "Sugestoes: " + substituicao.sugestoes());
    }

    @Entao("a substituicao deve ter sido aprovada automaticamente")
    public void aprovadaAutomaticamente() {
        assertTrue(substituicao.aprovadaAutomaticamente());
    }

    @Entao("o exercicio na posicao {int} da ficha nao deve ser {string}")
    public void exercicioNaoE(int posicao, String nome) {
        var ficha = consultas.fichasDoAluno(contexto.alunoId()).get(0);
        assertNotEquals(nome, ficha.itens().get(posicao).exercicio());
    }

    @Entao("o pedido de {string} deve aparecer antes do pedido de {string} na fila")
    public void ordemDaFila(String primeiro, String segundo) {
        List<String> nomes = substituicoes.todas().stream().filter(s -> "PENDENTE".equals(s.status()))
                .map(Respostas.SubstituicaoResumo::alunoNome).toList();
        assertTrue(nomes.indexOf(primeiro) >= 0 && nomes.indexOf(primeiro) < nomes.indexOf(segundo), "Fila: " + nomes);
    }

    /* ---------- atendimento ---------- */

    @Dado("um aluno com ficha chamado {string} com CPF {string} pede atendimento {string} com o motivo {string}")
    public void alunoPedeAtendimento(String nome, String cpf, String tipo, String motivo) {
        Long alunoId = alunoComFicha(nome, cpf, "HIPERTROFIA");
        atendimento = atendimentos.abrir(alunoId, new Comandos.AbrirAtendimento(null, motivo, tipo));
        atendimentoPorAluno.put(nome, atendimento.id());
    }

    @Quando("o instrutor {string} assumir o atendimento de {string}")
    public void assumirDe(String instrutor, String aluno) {
        atendimento = atendimentos.assumir(atendimentoPorAluno.get(aluno), instrutor);
    }

    @Quando("o instrutor {string} tentar assumir o atendimento de {string}")
    public void tentarAssumirDe(String instrutor, String aluno) {
        capturar(() -> assumirDe(instrutor, aluno));
    }

    private Respostas.AtendimentoResumo atendimentoDe(String aluno) {
        Long id = atendimentoPorAluno.get(aluno);
        return atendimentos.todos().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    }

    @Entao("o atendimento de {string} deve ser o {int}o da fila com prioridade {string}")
    public void posicaoNaFila(String aluno, int posicao, String prioridade) {
        var a = atendimentoDe(aluno);
        assertEquals(posicao, a.posicaoNaFila());
        assertEquals(prioridade, a.prioridade());
    }

    @Entao("o atendimento de {string} deve estar atrasado")
    public void atendimentoAtrasado(String aluno) {
        assertTrue(atendimentoDe(aluno).atrasado());
    }

    @Quando("o aluno avaliar o atendimento com nota {int} e comentario {string}")
    public void avaliarAtendimento(int nota, String comentario) {
        atendimento = atendimentos.avaliar(contexto.alunoId(), atendimento.id(),
                new Comandos.AvaliarAtendimento(nota, comentario));
    }

    @Quando("o aluno tentar avaliar o atendimento com nota {int} e comentario {string}")
    public void tentarAvaliarAtendimento(int nota, String comentario) {
        capturar(() -> avaliarAtendimento(nota, comentario));
    }

    @Quando("o aluno cancelar o atendimento")
    public void cancelarAtendimento() {
        atendimento = atendimentos.cancelar(contexto.alunoId(), atendimento.id());
    }

    @Quando("o atendimento for transferido para {string}")
    public void transferir(String instrutor) {
        capturar(() -> atendimento = atendimentos.transferir(atendimento.id(), instrutor));
    }

    @Entao("o atendimento deve estar com o instrutor {string}")
    public void comInstrutor(String instrutor) {
        assertEquals(instrutor, atendimento.instrutor());
        assertEquals(1, atendimento.transferencias());
    }

    /* ---------- feedback com carga ---------- */

    @Quando("o aluno registrar o feedback com esforco {int} por {int} minutos")
    public void feedbackComDuracao(int esforco, int minutos) {
        feedback = feedbacks.registrar(contexto.alunoId(),
                new Comandos.RegistrarFeedback(esforco, null, minutos, false, null));
    }

    @Quando("o aluno tentar registrar o feedback com esforco {int} por {int} minutos")
    public void tentarFeedbackComDuracao(int esforco, int minutos) {
        capturar(() -> feedbackComDuracao(esforco, minutos));
    }

    @Quando("o aluno registrar o feedback com esforco {int} por {int} minutos e dor no {string}")
    public void feedbackComDor(int esforco, int minutos, String local) {
        feedback = feedbacks.registrar(contexto.alunoId(),
                new Comandos.RegistrarFeedback(esforco, null, minutos, true, local));
    }

    @Quando("o aluno tentar registrar o feedback com esforco {int} por {int} minutos e dor no {string}")
    public void tentarFeedbackComDor(int esforco, int minutos, String local) {
        capturar(() -> feedbackComDor(esforco, minutos, local));
    }

    private void feedbackAntigo(int diasAtras, int esforco, int minutos) {
        repositorioDeFeedbacks.salvar(new school.cesar.gymflow.domain.treino.model.FeedbackDeTreino(null,
                contexto.alunoId(), contexto.ficha().id(), LocalDate.now().minusDays(diasAtras), esforco, null,
                1, 9, minutos, false, null, null));
    }

    @Dado("que o aluno registrou treinos com esforco {int} de {int} minutos nos ultimos {int} dias")
    public void treinosRecentes(int esforco, int minutos, int dias) {
        for (int d = 1; d <= dias; d++) {
            feedbackAntigo(d, esforco, minutos);
        }
    }

    @Dado("que o aluno registrou {int} treinos leves entre 8 e 27 dias atras")
    public void treinosLeves(int quantidade) {
        int[] dias = {8, 14, 20, 27};
        for (int i = 0; i < quantidade; i++) {
            feedbackAntigo(dias[i % dias.length], 3, 30);
        }
    }

    @Dado("eu concluir todos os itens da ficha")
    public void concluirTodos() {
        for (int i = 0; i < contexto.ficha().itens().size(); i++) {
            contexto.ficha(concluirItem.executar(new Comandos.ConcluirItem(contexto.ficha().id(), i)));
        }
    }

    @Entao("a carga do treino deve ser {int}")
    public void carga(int carga) {
        assertEquals(carga, feedback.carga());
    }

    @Entao("o alerta de carga deve ser {string}")
    public void alerta(String alerta) {
        assertEquals(alerta, feedback.alerta());
    }

    @Entao("o ACWR deve ser maior que {double}")
    public void acwr(double limite) {
        assertNotNull(feedback.acwr());
        assertTrue(feedback.acwr().doubleValue() > limite, "ACWR = " + feedback.acwr());
    }

    @Entao("o aluno deve ter um atendimento aberto automaticamente com prioridade {string}")
    public void atendimentoAutomatico(String prioridade) {
        var a = atendimentos.doAluno(contexto.alunoId()).stream()
                .filter(Respostas.AtendimentoResumo::automatico).findFirst()
                .orElseThrow(() -> new AssertionError("Nenhum atendimento aberto pelo evento DorRelatada"));
        assertTrue(a.automatico());
        assertEquals(prioridade, a.prioridade());
        assertEquals("ABERTO", a.status());
    }
}
