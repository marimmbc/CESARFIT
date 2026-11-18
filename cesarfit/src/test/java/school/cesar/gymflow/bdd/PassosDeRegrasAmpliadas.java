package school.cesar.gymflow.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.application.usecase.AlterarSenhaUseCase;
import school.cesar.gymflow.application.usecase.CadastrarAlunoUseCase;
import school.cesar.gymflow.application.usecase.EntrarNoSistemaUseCase;
import school.cesar.gymflow.application.usecase.GerenciadorDeSessoes;
import school.cesar.gymflow.application.usecase.MatricularAlunoUseCase;
import school.cesar.gymflow.application.usecase.RegistrarPagamentoUseCase;
import school.cesar.gymflow.application.usecase.RegistrarPresencaUseCase;
import school.cesar.gymflow.application.usecase.ServicoDeConsultas;
import school.cesar.gymflow.application.usecase.TrancamentoDeMatriculaUseCase;
import school.cesar.gymflow.domain.matricula.model.Objetivo;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.treino.repositorio.RepositorioDePresencas;

/** Steps das regras ampliadas de alunos, pagamentos, trancamento, acesso e check-in. */
public class PassosDeRegrasAmpliadas {

    @Autowired
    private ContextoDeTeste contexto;

    @Autowired
    private PassosComuns comuns;

    @Autowired
    private RelogioDeTeste relogio;

    @Autowired
    private CadastrarAlunoUseCase cadastrarAluno;

    @Autowired
    private MatricularAlunoUseCase matricular;

    @Autowired
    private RegistrarPagamentoUseCase registrarPagamento;

    @Autowired
    private TrancamentoDeMatriculaUseCase trancamento;

    @Autowired
    private ServicoDeConsultas consultas;

    @Autowired
    private EntrarNoSistemaUseCase entrar;

    @Autowired
    private AlterarSenhaUseCase alterarSenha;

    @Autowired
    private GerenciadorDeSessoes sessoes;

    @Autowired
    private RegistrarPresencaUseCase registrarPresenca;

    @Autowired
    private RepositorioDePresencas presencas;

    private Respostas.AlunoResumo alunoCadastrado;
    private Respostas.Sessao sessao;
    private LocalDate fimAntes;

    private void capturar(Runnable acao) {
        try {
            acao.run();
        } catch (RegraDeNegocioException e) {
            contexto.erro(e);
        }
    }

    /* ---------- alunos ---------- */

    private void cadastrarMenor(int idade, String nome, String cpf, String responsavel, String cpfResponsavel) {
        alunoCadastrado = cadastrarAluno.executar(new Comandos.SalvarAluno(nome, cpf, "81988887777",
                nome.toLowerCase().replace(" ", ".") + "@menor.com", LocalDate.now().minusYears(idade).minusDays(10),
                Objetivo.CONDICIONAMENTO, responsavel, cpfResponsavel));
        contexto.alunoId(alunoCadastrado.id());
    }

    @Quando("eu tentar cadastrar um aluno de {int} anos chamado {string} com CPF {string} sem responsavel")
    public void menorSemResponsavel(int idade, String nome, String cpf) {
        capturar(() -> cadastrarMenor(idade, nome, cpf, null, null));
    }

    @Quando("eu cadastrar um aluno de {int} anos chamado {string} com CPF {string} e responsavel {string} com CPF {string}")
    public void menorComResponsavel(int idade, String nome, String cpf, String responsavel, String cpfResponsavel) {
        cadastrarMenor(idade, nome, cpf, responsavel, cpfResponsavel);
    }

    @Quando("eu tentar cadastrar um aluno de {int} anos chamado {string} com CPF {string} e responsavel {string} com CPF {string}")
    public void tentarMenorComResponsavel(int idade, String nome, String cpf, String responsavel, String cpfResponsavel) {
        capturar(() -> cadastrarMenor(idade, nome, cpf, responsavel, cpfResponsavel));
    }

    @Entao("o aluno deve ser menor de idade com o responsavel {string}")
    public void menorComResponsavel(String responsavel) {
        assertTrue(alunoCadastrado.menorDeIdade());
        assertEquals(responsavel, alunoCadastrado.responsavelNome());
    }

    @Quando("eu tentar cadastrar outro aluno com o mesmo e-mail e CPF {string}")
    public void mesmoEmail(String cpf) {
        String email = consultas.alunos(null).stream().filter(a -> a.id().equals(contexto.alunoId()))
                .findFirst().orElseThrow().email();
        capturar(() -> cadastrarAluno.executar(new Comandos.SalvarAluno("Outra Pessoa", cpf, "81988887777", email,
                LocalDate.of(1990, 1, 1), Objetivo.HIPERTROFIA)));
    }

    @Entao("a matricula deve continuar no historico em nome de um aluno anonimizado")
    public void historicoMantido() {
        var m = consultas.matricula(contexto.matricula().id());
        assertTrue(m.alunoNome().startsWith("Aluno removido #"), m.alunoNome());
        assertFalse(m.pagamentos().isEmpty());
    }

    /* ---------- pagamentos ---------- */

    @Quando("eu registrar um pagamento de {string} via {string} em {int} vezes")
    public void pagarParcelado(String valor, String forma, int parcelas) {
        capturar(() -> contexto.matricula(registrarPagamento.executar(new Comandos.RegistrarPagamento(
                contexto.matricula().id(), valor, forma, parcelas))));
    }

    @Quando("chegar o dia {int} apos o fim da matricula")
    public void diaAposOFim(int dias) {
        fimAntes = contexto.matricula().fim();
        relogio.posicionarEm(fimAntes.plusDays(dias).atTime(10, 0));
    }

    private Respostas.PagamentoResumo ultimoPagamento() {
        var pagamentos = consultas.matricula(contexto.matricula().id()).pagamentos();
        assertFalse(pagamentos.isEmpty(), "Nenhum pagamento registrado");
        return pagamentos.get(pagamentos.size() - 1);
    }

    @Entao("o ultimo pagamento deve ter troco de {string}")
    public void troco(String valor) {
        assertEquals(valor, ultimoPagamento().troco());
    }

    @Entao("o ultimo pagamento deve ter {int} parcelas")
    public void parcelas(int parcelas) {
        assertEquals(parcelas, ultimoPagamento().parcelas());
    }

    @Entao("a vigencia deve ter sido renovada a partir do vencimento anterior")
    public void vigenciaRenovada() {
        assertNotNull(fimAntes);
        assertEquals(fimAntes.plusMonths(1), contexto.matricula().fim());
    }

    @Entao("o credito de troca deve ser {string}")
    public void creditoDeTroca(String valor) {
        assertEquals(valor, consultas.matricula(contexto.matricula().id()).creditoDeTroca());
    }

    @Entao("o valor devido deve ser {string}")
    public void valorDevido(String valor) {
        assertEquals(valor, consultas.matricula(contexto.matricula().id()).valorDevido());
    }

    @Entao("a matricula nao deve ter pagamento pendente")
    public void semPendencia() {
        assertNotNull(contexto.matricula());
        assertFalse(consultas.matricula(contexto.matricula().id()).pagamentoPendente());
    }

    /* ---------- trancamento ---------- */

    @Dado("um aluno anual pago chamado {string} com CPF {string}")
    public void alunoAnualPago(String nome, String cpf) {
        Long alunoId = comuns.cadastrar(nome, cpf, Objetivo.HIPERTROFIA);
        contexto.alunoId(alunoId);
        contexto.matricula(matricular.executar(new Comandos.SalvarMatricula(alunoId, "ANUAL", LocalDate.now(), null)));
        contexto.matricula(registrarPagamento.executar(new Comandos.RegistrarPagamento(
                contexto.matricula().id(), "1320,00", "CARTAO_CREDITO", 12)));
    }

    @Quando("eu trancar a matricula por {int} dias")
    public void trancar(int dias) {
        if (fimAntes == null) {
            fimAntes = contexto.matricula().fim();
        }
        contexto.matricula(trancamento.trancar(contexto.matricula().id(), dias));
    }

    @Quando("eu tentar trancar a matricula por {int} dias")
    public void tentarTrancar(int dias) {
        capturar(() -> trancar(dias));
    }

    @Quando("eu destrancar a matricula")
    public void destrancar() {
        contexto.matricula(trancamento.destrancar(contexto.matricula().id()));
    }

    @Entao("o fim da matricula deve ter sido estendido em {int} dias")
    public void fimEstendido(int dias) {
        assertEquals(fimAntes.plusDays(dias), consultas.matricula(contexto.matricula().id()).fim());
    }

    /* ---------- acesso ---------- */

    private String numero() {
        return String.valueOf(contexto.matricula().id());
    }

    @Quando("o aluno errar a senha {int} vezes")
    public void errarSenha(int vezes) {
        for (int i = 0; i < vezes; i++) {
            capturar(() -> entrar.executar(new Comandos.Entrar(numero(), "senha-errada")));
        }
    }

    @Entao("o sistema deve pedir a troca da senha")
    public void pedeTroca() {
        Respostas.Sessao s = entrar.executar(new Comandos.Entrar(numero(), senhaInicial()));
        assertTrue(s.precisaTrocarSenha());
    }

    private String senhaInicial() {
        return consultas.alunos(null).stream().filter(a -> a.id().equals(contexto.alunoId())).findFirst()
                .orElseThrow().cpf().replaceAll("\\D", "").substring(0, 6);
    }

    @Quando("o aluno tentar trocar a senha {string} pela nova senha {string}")
    public void tentarTrocarSenha(String atual, String nova) {
        capturar(() -> alterarSenha.executar(contexto.alunoId(), new Comandos.AlterarSenha(atual, nova)));
    }

    @Dado("o aluno entrar e guardar a sessao")
    public void entrarEGuardar() {
        sessao = entrar.executar(new Comandos.Entrar(numero(), senhaInicial()));
    }

    @Entao("a sessao deve continuar valida")
    public void sessaoValida() {
        garantirSessao();
        assertTrue(sessoes.buscar(sessao.token()).isPresent());
    }

    @Entao("a sessao deve ter expirado")
    public void sessaoExpirada() {
        garantirSessao();
        assertTrue(sessoes.buscar(sessao.token()).isEmpty());
    }

    /** O login do cenario e feito pelo step comum; aqui pegamos uma sessao aberta no mesmo instante. */
    private void garantirSessao() {
        if (sessao == null) {
            throw new AssertionError("Use 'o aluno entrar e guardar a sessao' antes de verificar a sessao");
        }
    }

    /* ---------- check-in ---------- */

    @Dado("que o relogio marca hoje as {int}h")
    public void relogioHoje(int hora) {
        relogio.posicionarEm(LocalDate.now().atTime(hora, 0));
    }

    @Quando("o aluno tentar fazer o check-in")
    public void tentarCheckIn() {
        capturar(() -> registrarPresenca.executar(contexto.alunoId()));
    }

    @Entao("o check-in do dia deve estar registrado")
    public void checkInRegistrado() {
        assertTrue(presencas.existe(contexto.alunoId(), relogio.hoje()));
    }
}
