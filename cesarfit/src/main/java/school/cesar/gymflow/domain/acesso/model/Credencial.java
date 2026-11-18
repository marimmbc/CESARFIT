package school.cesar.gymflow.domain.acesso.model;

import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

/**
 * Raiz de agregado: Credencial.
 * Guarda a senha (sempre codificada) de um aluno ou de um membro da equipe.
 * O aluno entra com o numero da matricula; a equipe entra com o seu numero de acesso.
 */
public class Credencial {

    public static final int TAMANHO_MINIMO_DA_SENHA = 8;

    private Long id;
    private final String login;
    private final Long alunoId;
    private final Perfil perfil;
    private final String nome;
    private String senhaCodificada;

    public Credencial(Long id, String login, Long alunoId, Perfil perfil, String nome, String senhaCodificada) {
        if (perfil == null) {
            throw new RegraDeNegocioException("Perfil de acesso e obrigatorio");
        }
        if (perfil == Perfil.ALUNO && alunoId == null) {
            throw new RegraDeNegocioException("Credencial de aluno exige o aluno");
        }
        if (perfil == Perfil.EQUIPE && (login == null || login.isBlank())) {
            throw new RegraDeNegocioException("Credencial da equipe exige o numero de acesso");
        }
        if (senhaCodificada == null || senhaCodificada.isBlank()) {
            throw new RegraDeNegocioException("Senha e obrigatoria");
        }
        this.id = id;
        this.login = login == null ? null : login.trim().toUpperCase();
        this.alunoId = alunoId;
        this.perfil = perfil;
        this.nome = nome;
        this.senhaCodificada = senhaCodificada;
    }

    public static Credencial doAluno(Long alunoId, String senhaCodificada) {
        return new Credencial(null, null, alunoId, Perfil.ALUNO, null, senhaCodificada);
    }

    public static Credencial daEquipe(String login, String nome, String senhaCodificada) {
        return new Credencial(null, login, null, Perfil.EQUIPE, nome, senhaCodificada);
    }

    /** Regra da senha em texto puro, verificada antes de codificar. */
    public static void validarNovaSenha(String senha) {
        validarNovaSenha(senha, null);
    }

    /**
     * Senha forte: pelo menos 8 caracteres, com letras e numeros, e sem conter
     * a senha de primeiro acesso (os 6 primeiros digitos do CPF).
     */
    public static void validarNovaSenha(String senha, String cpf) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_DA_SENHA) {
            throw new RegraDeNegocioException("A nova senha deve ter pelo menos "
                    + TAMANHO_MINIMO_DA_SENHA + " caracteres");
        }
        if (!senha.matches(".*[A-Za-z].*") || !senha.matches(".*\\d.*")) {
            throw new RegraDeNegocioException("A nova senha deve ter letras e numeros");
        }
        if (cpf != null && cpf.length() >= 6 && senha.contains(cpf.substring(0, 6))) {
            throw new RegraDeNegocioException("A nova senha nao pode conter os digitos do seu CPF");
        }
    }

    public void trocarSenha(String novaSenhaCodificada) {
        if (novaSenhaCodificada == null || novaSenhaCodificada.isBlank()) {
            throw new RegraDeNegocioException("Senha e obrigatoria");
        }
        this.senhaCodificada = novaSenhaCodificada;
    }

    public void atribuirId(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }

    public String login() {
        return login;
    }

    public Long alunoId() {
        return alunoId;
    }

    public Perfil perfil() {
        return perfil;
    }

    public String nome() {
        return nome;
    }

    public String senhaCodificada() {
        return senhaCodificada;
    }
}
