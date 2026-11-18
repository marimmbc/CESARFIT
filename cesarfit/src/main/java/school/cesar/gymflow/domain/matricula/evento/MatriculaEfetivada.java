package school.cesar.gymflow.domain.matricula.evento;

import java.time.LocalDate;

import school.cesar.gymflow.domain.shared.evento.Evento;

/** Contexto Matricula: um aluno foi matriculado. O contexto Treino reage gerando a ficha. */
public record MatriculaEfetivada(Long matriculaId, Long alunoId, String plano, LocalDate inicio, LocalDate fim) implements Evento {

    @Override
    public String referencia() {
        return String.valueOf(matriculaId);
    }

    @Override
    public String descricao() {
        return "Matricula " + matriculaId + " do aluno " + alunoId + " efetivada no " + plano;
    }
}
