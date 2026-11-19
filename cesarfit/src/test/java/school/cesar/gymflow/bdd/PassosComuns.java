package school.cesar.gymflow.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import io.cucumber.java.Before;
import io.cucumber.java.pt.Entao;
import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.usecase.CadastrarAlunoUseCase;
import school.cesar.gymflow.domain.matricula.model.Objetivo;
import school.cesar.gymflow.infrastructure.config.CatalogoInicial;

/** Steps reutilizados por mais de uma funcionalidade. */
public class PassosComuns {

    @Autowired
    private ContextoDeTeste contexto;

    @Autowired
    private CadastrarAlunoUseCase cadastrarAluno;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CatalogoInicial catalogoInicial;

    /**
     * Cada cenario comeca com o banco vazio (so com o catalogo padrao de exercicios),
     * para que dados gravados por um cenario nao interfiram no proximo.
     */
    @Before(order = 0)
    public void prepararCenario() {
        contexto.limpar();
        List<String> tabelas = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'"
                        + " AND TABLE_TYPE = 'BASE TABLE'", String.class);
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            tabelas.forEach(t -> jdbc.execute("TRUNCATE TABLE \"" + t + "\" RESTART IDENTITY"));
        } finally {
            jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
        catalogoInicial.run();
    }

    Long cadastrar(String nome, String cpf, Objetivo objetivo) {
        var resumo = cadastrarAluno.executar(new Comandos.SalvarAluno(
                nome, cpf, "81988887777", nome.toLowerCase().replace(" ", ".") + "." + cpf.replaceAll("\\D", "") + "@email.com",
                LocalDate.of(1995, 5, 10), objetivo));
        assertNotNull(resumo.id());
        return resumo.id();
    }

    @Entao("devo receber um erro contendo {string}")
    public void devoReceberErroContendo(String trecho) {
        assertNotNull(contexto.erro(), "Nenhum erro foi capturado no cenario");
        org.junit.jupiter.api.Assertions.assertTrue(contexto.erro().getMessage().contains(trecho),
                "Mensagem recebida: " + contexto.erro().getMessage());
    }

    @Entao("devo receber o erro {string}")
    public void devoReceberOErro(String mensagem) {
        assertNotNull(contexto.erro(), "Nenhum erro foi capturado no cenario");
        assertEquals(mensagem, contexto.erro().getMessage());
    }
}
