package school.cesar.gymflow.application.usecase;

import school.cesar.gymflow.application.Transacional;
import school.cesar.gymflow.application.dto.Respostas;

/** Junta, para a area do aluno, os avisos, feedbacks e solicitacoes dele. */
public class SolicitacoesDoAlunoUseCase {

    private final NotificacaoDeVencimentoUseCase avisos;
    private final FeedbackPosTreinoUseCase feedbacks;
    private final AvaliacaoFisicaUseCase avaliacoes;
    private final SubstituicaoDeExercicioUseCase substituicoes;
    private final AtendimentoDoInstrutorUseCase atendimentos;
    private final ContestacaoDeCobrancaUseCase contestacoes;

    public SolicitacoesDoAlunoUseCase(NotificacaoDeVencimentoUseCase avisos, FeedbackPosTreinoUseCase feedbacks,
                                      AvaliacaoFisicaUseCase avaliacoes, SubstituicaoDeExercicioUseCase substituicoes,
                                      AtendimentoDoInstrutorUseCase atendimentos,
                                      ContestacaoDeCobrancaUseCase contestacoes) {
        this.avisos = avisos;
        this.feedbacks = feedbacks;
        this.avaliacoes = avaliacoes;
        this.substituicoes = substituicoes;
        this.atendimentos = atendimentos;
        this.contestacoes = contestacoes;
    }

    @Transacional(somenteLeitura = true)
    public Respostas.SolicitacoesDoAluno executar(Long alunoId) {
        return new Respostas.SolicitacoesDoAluno(avisos.doAluno(alunoId), feedbacks.doAluno(alunoId),
                feedbacks.registradoHoje(alunoId), avaliacoes.doAluno(alunoId), substituicoes.doAluno(alunoId),
                atendimentos.doAluno(alunoId), contestacoes.doAluno(alunoId));
    }
}
