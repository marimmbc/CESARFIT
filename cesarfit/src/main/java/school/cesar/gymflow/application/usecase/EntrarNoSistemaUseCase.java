package school.cesar.gymflow.application.usecase;

import school.cesar.gymflow.domain.acesso.model.TentativasDeAcesso;
import school.cesar.gymflow.domain.shared.Relogio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.acesso.model.Credencial;
import school.cesar.gymflow.domain.acesso.model.Perfil;
import school.cesar.gymflow.domain.acesso.repositorio.RepositorioDeCredenciais;
import school.cesar.gymflow.domain.acesso.servico.CodificadorDeSenha;
import school.cesar.gymflow.domain.matricula.model.Aluno;
import school.cesar.gymflow.domain.matricula.model.Matricula;
import school.cesar.gymflow.domain.matricula.model.StatusMatricula;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeAlunos;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeMatriculas;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

/**
 * Caso de uso: entrar no sistema com numero da matricula e senha.
 * Numero so com digitos = matricula de aluno; numero com letras (ex.: EQ001) = equipe.
 * Enquanto o aluno nao troca a senha, a senha de primeiro acesso sao os 6 primeiros digitos do CPF.
 */
@Service
public class EntrarNoSistemaUseCase {

    static final String CREDENCIAIS_INVALIDAS = "Numero da matricula ou senha invalidos";
    /** Aluno com matricula vencida ainda entra por 30 dias (para ver o financeiro e renovar). */
    public static final int DIAS_DE_ACESSO_APOS_VENCIMENTO = 30;

    /** Falha de credencial: conta para o bloqueio por forca bruta. */
    private static final class SenhaIncorreta extends RuntimeException {
        SenhaIncorreta() {
            super(CREDENCIAIS_INVALIDAS, null, false, false);
        }
    }

    private final RepositorioDeCredenciais credenciais;
    private final RepositorioDeMatriculas matriculas;
    private final RepositorioDeAlunos alunos;
    private final CodificadorDeSenha codificador;
    private final GerenciadorDeSessoes sessoes;
    private final Relogio relogio;

    public EntrarNoSistemaUseCase(RepositorioDeCredenciais credenciais, RepositorioDeMatriculas matriculas,
                                  RepositorioDeAlunos alunos, CodificadorDeSenha codificador,
                                  GerenciadorDeSessoes sessoes, Relogio relogio) {
        this.credenciais = credenciais;
        this.matriculas = matriculas;
        this.alunos = alunos;
        this.codificador = codificador;
        this.sessoes = sessoes;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Respostas.Sessao executar(Comandos.Entrar comando) {
        String numero = comando.numero() == null ? "" : comando.numero().trim();
        String senha = comando.senha() == null ? "" : comando.senha();
        if (numero.isEmpty() || senha.isEmpty()) {
            throw new RegraDeNegocioException("Informe o numero da matricula e a senha");
        }
        TentativasDeAcesso controle = sessoes.tentativasDe(numero);
        controle.exigirLiberado(relogio.agora());
        GerenciadorDeSessoes.SessaoAtiva sessao;
        try {
            sessao = numero.matches("\\d+")
                    ? entrarComoAluno(Long.parseLong(numero), senha)
                    : entrarComoEquipe(numero, senha);
        } catch (SenhaIncorreta e) {
            if (controle.registrarFalha(relogio.agora())) {
                throw new RegraDeNegocioException("Acesso bloqueado por " + TentativasDeAcesso.MINUTOS_DE_BLOQUEIO
                        + " minutos apos " + TentativasDeAcesso.LIMITE_DE_FALHAS + " tentativas invalidas");
            }
            throw new RegraDeNegocioException(CREDENCIAIS_INVALIDAS);
        }
        controle.registrarSucesso();
        boolean precisaTrocar = sessao.perfil() == Perfil.ALUNO && credenciais.doAluno(sessao.alunoId()).isEmpty();
        return new Respostas.Sessao(sessao.token(), sessao.perfil().name(), sessao.nome(), sessao.alunoId(),
                precisaTrocar);
    }

    private GerenciadorDeSessoes.SessaoAtiva entrarComoAluno(Long numeroDaMatricula, String senha) {
        Matricula matricula = matriculas.porId(numeroDaMatricula).orElseThrow(SenhaIncorreta::new);
        Aluno aluno = alunos.porId(matricula.alunoId()).filter(a -> !a.anonimizado()).orElseThrow(SenhaIncorreta::new);
        boolean senhaConfere = credenciais.doAluno(aluno.id())
                .map(c -> codificador.confere(senha, c.senhaCodificada()))
                .orElseGet(() -> senha.equals(senhaDePrimeiroAcesso(aluno)));
        if (!senhaConfere) {
            throw new SenhaIncorreta();
        }
        if (matricula.status() == StatusMatricula.CANCELADA) {
            throw new RegraDeNegocioException("Matricula cancelada: procure a recepcao");
        }
        if (matricula.diasDeAtraso(relogio.hoje()) > DIAS_DE_ACESSO_APOS_VENCIMENTO) {
            throw new RegraDeNegocioException("Matricula vencida ha mais de " + DIAS_DE_ACESSO_APOS_VENCIMENTO
                    + " dias: procure a recepcao para renovar");
        }
        return sessoes.abrir(Perfil.ALUNO, aluno.id(), aluno.nome());
    }

    private GerenciadorDeSessoes.SessaoAtiva entrarComoEquipe(String numero, String senha) {
        Credencial credencial = credenciais.daEquipe(numero)
                .filter(c -> codificador.confere(senha, c.senhaCodificada()))
                .orElseThrow(SenhaIncorreta::new);
        return sessoes.abrir(Perfil.EQUIPE, null, credencial.nome());
    }

    static String senhaDePrimeiroAcesso(Aluno aluno) {
        return aluno.cpf().numero().substring(0, 6);
    }
}
