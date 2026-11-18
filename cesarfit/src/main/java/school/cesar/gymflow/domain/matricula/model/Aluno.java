package school.cesar.gymflow.domain.matricula.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

import school.cesar.gymflow.domain.shared.Cpf;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

/** Raiz de agregado: Aluno. Todas as regras de validacao do cadastro ficam aqui. */
public class Aluno {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int IDADE_MINIMA = 16;
    public static final int MAIORIDADE = 18;
    public static final int IDADE_MAXIMA = 100;

    private Long id;
    private String nome;
    private Cpf cpf;
    private String telefone;
    private String email;
    private LocalDate dataDeNascimento;
    private Objetivo objetivo;
    private String responsavelNome;
    private Cpf responsavelCpf;
    private boolean anonimizado;

    public Aluno(Long id, String nome, Cpf cpf, String telefone, String email,
                 LocalDate dataDeNascimento, Objetivo objetivo) {
        this(id, nome, cpf, telefone, email, dataDeNascimento, objetivo, null, null);
    }

    public Aluno(Long id, String nome, Cpf cpf, String telefone, String email,
                 LocalDate dataDeNascimento, Objetivo objetivo, String responsavelNome, Cpf responsavelCpf) {
        this.id = id;
        aplicar(nome, cpf, telefone, email, dataDeNascimento, objetivo);
        aplicarResponsavel(responsavelNome, responsavelCpf);
    }

    private Aluno() {
    }

    /** Usado pela persistencia para um aluno ja anonimizado (dados fora das regras de cadastro). */
    public static Aluno restaurarAnonimizado(Long id, String nome, Cpf cpf, String telefone, String email,
                                             LocalDate dataDeNascimento, Objetivo objetivo) {
        Aluno a = new Aluno();
        a.id = id;
        a.nome = nome;
        a.cpf = cpf;
        a.telefone = telefone;
        a.email = email;
        a.dataDeNascimento = dataDeNascimento;
        a.objetivo = objetivo;
        a.anonimizado = true;
        return a;
    }

    /** Edicao completa dos dados do aluno (mesmas validacoes do cadastro). */
    public void atualizar(String nome, Cpf cpf, String telefone, String email,
                          LocalDate dataDeNascimento, Objetivo objetivo) {
        atualizar(nome, cpf, telefone, email, dataDeNascimento, objetivo, responsavelNome, responsavelCpf);
    }

    public void atualizar(String nome, Cpf cpf, String telefone, String email, LocalDate dataDeNascimento,
                          Objetivo objetivo, String nomeDoResponsavel, Cpf cpfDoResponsavel) {
        if (anonimizado) {
            throw new RegraDeNegocioException("Aluno anonimizado nao pode ser editado");
        }
        aplicar(nome, cpf, telefone, email, dataDeNascimento, objetivo);
        aplicarResponsavel(nomeDoResponsavel, cpfDoResponsavel);
    }

    /**
     * Menor de 18 anos so pode ser cadastrado com um responsavel legal (nome e CPF diferente do aluno).
     * Maior de idade nao guarda responsavel.
     */
    private void aplicarResponsavel(String nomeDoResponsavel, Cpf cpfDoResponsavel) {
        if (!menorDeIdade()) {
            this.responsavelNome = null;
            this.responsavelCpf = null;
            return;
        }
        if (nomeDoResponsavel == null || nomeDoResponsavel.trim().length() < 3 || cpfDoResponsavel == null) {
            throw new RegraDeNegocioException("Aluno menor de " + MAIORIDADE
                    + " anos exige nome e CPF do responsavel legal");
        }
        if (cpfDoResponsavel.equals(cpf)) {
            throw new RegraDeNegocioException("O CPF do responsavel deve ser diferente do CPF do aluno");
        }
        this.responsavelNome = nomeDoResponsavel.trim();
        this.responsavelCpf = cpfDoResponsavel;
    }

    public boolean menorDeIdade() {
        return idade() < MAIORIDADE;
    }

    /**
     * LGPD: aluno com historico financeiro nao pode ser apagado (o historico precisa ser guardado),
     * entao os dados pessoais sao substituidos e o aluno some das listagens.
     */
    public void anonimizar() {
        if (anonimizado) {
            throw new RegraDeNegocioException("Aluno ja foi anonimizado");
        }
        String sufixo = String.format("%08d", id == null ? 0 : id);
        this.nome = "Aluno removido #" + id;
        this.cpf = Cpf.de("999" + sufixo);
        this.telefone = "00000000000";
        this.email = "removido-" + id + "@cesarfit.invalid";
        this.responsavelNome = null;
        this.responsavelCpf = null;
        this.anonimizado = true;
    }

    private void aplicar(String nome, Cpf cpf, String telefone, String email,
                         LocalDate dataDeNascimento, Objetivo objetivo) {
        if (nome == null || nome.trim().length() < 3) {
            throw new RegraDeNegocioException("Nome do aluno e obrigatorio (minimo 3 letras)");
        }
        if (cpf == null) {
            throw new RegraDeNegocioException("CPF e obrigatorio");
        }
        String digitosTelefone = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (digitosTelefone.length() < 10 || digitosTelefone.length() > 11) {
            throw new RegraDeNegocioException("Telefone invalido: informe DDD + numero (10 ou 11 digitos)");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new RegraDeNegocioException("E-mail invalido: " + (email == null ? "" : email));
        }
        if (dataDeNascimento == null || dataDeNascimento.isAfter(LocalDate.now())) {
            throw new RegraDeNegocioException("Data de nascimento invalida");
        }
        if (Period.between(dataDeNascimento, LocalDate.now()).getYears() < IDADE_MINIMA) {
            throw new RegraDeNegocioException("Idade minima para cadastro e " + IDADE_MINIMA + " anos");
        }
        if (Period.between(dataDeNascimento, LocalDate.now()).getYears() > IDADE_MAXIMA) {
            throw new RegraDeNegocioException("Data de nascimento invalida: idade acima de " + IDADE_MAXIMA + " anos");
        }
        this.nome = nome.trim();
        this.cpf = cpf;
        this.telefone = digitosTelefone;
        this.email = email.trim().toLowerCase();
        this.dataDeNascimento = dataDeNascimento;
        this.objetivo = objetivo == null ? Objetivo.CONDICIONAMENTO : objetivo;
    }

    public int idade() {
        return Period.between(dataDeNascimento, LocalDate.now()).getYears();
    }

    public String telefoneFormatado() {
        if (telefone.length() == 11) {
            return "(" + telefone.substring(0, 2) + ") " + telefone.substring(2, 7) + "-" + telefone.substring(7);
        }
        return "(" + telefone.substring(0, 2) + ") " + telefone.substring(2, 6) + "-" + telefone.substring(6);
    }

    public void redefinirObjetivo(Objetivo novoObjetivo) {
        if (novoObjetivo == null) {
            throw new RegraDeNegocioException("Objetivo e obrigatorio");
        }
        this.objetivo = novoObjetivo;
    }

    public void atribuirId(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public Cpf cpf() {
        return cpf;
    }

    public String telefone() {
        return telefone;
    }

    public String email() {
        return email;
    }

    public LocalDate dataDeNascimento() {
        return dataDeNascimento;
    }

    public Objetivo objetivo() {
        return objetivo;
    }

    public String responsavelNome() {
        return responsavelNome;
    }

    public Cpf responsavelCpf() {
        return responsavelCpf;
    }

    public boolean anonimizado() {
        return anonimizado;
    }
}
