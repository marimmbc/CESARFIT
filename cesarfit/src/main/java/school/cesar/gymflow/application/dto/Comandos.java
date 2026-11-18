package school.cesar.gymflow.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import school.cesar.gymflow.domain.matricula.model.Objetivo;
import school.cesar.gymflow.domain.shared.Cpf;

/** Comandos de entrada dos casos de uso (camada de aplicacao). */
public final class Comandos {

    private Comandos() {
    }

    /** Usado tanto no cadastro quanto na edicao do aluno. */
    /** Responsavel (nome e CPF) obrigatorio para menores de 18 anos. */
    public record SalvarAluno(String nome, String cpf, String telefone, String email,
                              LocalDate dataDeNascimento, Objetivo objetivo,
                              String responsavelNome, String responsavelCpf) {
        public SalvarAluno(String nome, String cpf, String telefone, String email,
                           LocalDate dataDeNascimento, Objetivo objetivo) {
            this(nome, cpf, telefone, email, dataDeNascimento, objetivo, null, null);
        }

        public Cpf cpfDoResponsavel() {
            return responsavelCpf == null || responsavelCpf.isBlank() ? null : Cpf.de(responsavelCpf);
        }
    }

    /**
     * Usado no cadastro e na edicao da matricula. O valor NAO vem da interface:
     * e sempre calculado no servidor a partir do plano escolhido.
     */
    public record SalvarMatricula(Long alunoId, String plano, LocalDate inicio, String status) {
    }

    /** parcelas: so no cartao de credito e no plano Anual (padrao 1). */
    public record RegistrarPagamento(Long matriculaId, String valor, String formaDePagamento, Integer parcelas) {
        public RegistrarPagamento(Long matriculaId, String valor, String formaDePagamento) {
            this(matriculaId, valor, formaDePagamento, null);
        }
    }

    public record Trancar(Integer dias) {
    }

    public record ConcluirItem(Long fichaId, int posicao) {
    }

    public record Entrar(String numero, String senha) {
    }

    public record AlterarSenha(String senhaAtual, String novaSenha) {
    }

    public record SalvarExercicio(String nome, String grupoMuscular, String equipamento) {
    }

    /** Texto livre: justificativa, motivo, solucao, parecer ou orientacao. */
    public record Texto(String texto) {
    }

    /** valor vazio = estorno integral do saldo; categoria: ver MotivoDeEstorno (padrao OUTRO). */
    public record EstornarPagamento(String valor, String categoria, String motivo) {
    }

    public record AbrirContestacao(Long matriculaId, int indiceDoPagamento, String motivo) {
    }

    /**
     * resultado: PROCEDENTE, PARCIALMENTE_PROCEDENTE ou IMPROCEDENTE (se vazio, usa "procedente").
     * valorParcial: obrigatorio no resultado parcial.
     */
    public record AnalisarContestacao(Boolean procedente, String resultado, String valorParcial, String parecer) {
        public AnalisarContestacao(boolean procedente, String parecer) {
            this(procedente, null, null, parecer);
        }
    }

    /** Severidade: BAIXA, MEDIA (padrao), ALTA ou CRITICA. */
    public record RegistrarOcorrencia(String equipamento, String descricao, String severidade) {
        public RegistrarOcorrencia(String equipamento, String descricao) {
            this(equipamento, descricao, null);
        }
    }

    /** Custo obrigatorio quando a ocorrencia aguardou peca. */
    public record ResolverOcorrencia(String solucao, String custo) {
    }

    public record SolicitarAvaliacao(LocalDateTime dataHora) {
    }

    public record RegistrarResultadoDeAvaliacao(Double pesoKg, Double alturaM, Double percentualDeGordura) {
    }

    public record ResponderAvaliacao(LocalDateTime novaDataHora, String observacao) {
    }

    /** categoria: DOR_OU_LESAO, EQUIPAMENTO_INDISPONIVEL, DIFICULDADE_TECNICA ou PREFERENCIA (padrao). */
    public record SolicitarSubstituicao(Long fichaId, int posicao, String motivo, String categoria) {
        public SolicitarSubstituicao(Long fichaId, int posicao, String motivo) {
            this(fichaId, posicao, motivo, null);
        }
    }

    public record AprovarSubstituicao(Long exercicioId, String observacao) {
    }

    /** tipo: DUVIDA_EXECUCAO (padrao), AJUSTE_DE_EQUIPAMENTO, DOR_OU_LESAO ou OUTRO. */
    public record AbrirAtendimento(Integer posicao, String motivo, String tipo) {
        public AbrirAtendimento(Integer posicao, String motivo) {
            this(posicao, motivo, null);
        }
    }

    public record AvaliarAtendimento(Integer nota, String comentario) {
    }

    /** duracaoMinutos: 10 a 240 (padrao 60). dor = true exige localDaDor. */
    public record RegistrarFeedback(int esforco, String observacao, Integer duracaoMinutos, Boolean dor,
                                    String localDaDor) {
        public RegistrarFeedback(int esforco, String observacao) {
            this(esforco, observacao, null, null, null);
        }
    }

    public record AdicionarExercicioNaFicha(Long exercicioId, String divisao, int series, int repeticoes) {
    }

    public record AjustarItemDaFicha(int series, int repeticoes) {
    }
}
