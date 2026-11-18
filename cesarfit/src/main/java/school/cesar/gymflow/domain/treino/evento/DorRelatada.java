package school.cesar.gymflow.domain.treino.evento;

import school.cesar.gymflow.domain.shared.evento.Evento;

/** Contexto Treino: o aluno relatou dor no feedback. O contexto Atendimento abre um chamado prioritario. */
public record DorRelatada(Long feedbackId, Long alunoId, String localDaDor) implements Evento {

    @Override
    public String referencia() {
        return String.valueOf(feedbackId);
    }

    @Override
    public String descricao() {
        return "Aluno " + alunoId + " relatou dor em: " + localDaDor;
    }
}
