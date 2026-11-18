package school.cesar.gymflow.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;
import school.cesar.gymflow.domain.shared.evento.BarramentoDeEventos;
import school.cesar.gymflow.domain.treino.model.AlertaDeCarga;
import school.cesar.gymflow.domain.treino.model.FeedbackDeTreino;
import school.cesar.gymflow.domain.treino.model.FichaDeTreino;
import school.cesar.gymflow.domain.treino.repositorio.RepositorioDeFeedbacks;
import school.cesar.gymflow.domain.treino.repositorio.RepositorioDeFichas;
import school.cesar.gymflow.domain.treino.repositorio.RepositorioDePresencas;
import school.cesar.gymflow.domain.treino.servico.AnaliseDeCarga;

/**
 * Funcionalidade: feedback pos-treino com analise de carga.
 * - exige check-in no dia e ao menos um exercicio concluido; um feedback por dia;
 * - calcula a carga (sRPE), a carga aguda/cronica e o ACWR, e classifica o alerta;
 * - relato de dor publica o evento DorRelatada (o contexto Atendimento abre um chamado PRIORITARIO);
 * - qualquer alerta publica o evento ALERTA_DE_CARGA para a equipe.
 */
@Service
public class FeedbackPosTreinoUseCase {

    private final RepositorioDeFeedbacks feedbacks;
    private final RepositorioDeFichas fichas;
    private final RepositorioDePresencas presencas;
    private final BarramentoDeEventos barramento;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;

    public FeedbackPosTreinoUseCase(RepositorioDeFeedbacks feedbacks, RepositorioDeFichas fichas,
                                    RepositorioDePresencas presencas, BarramentoDeEventos barramento,
                                    PublicadorDeEventos publicador, Relogio relogio) {
        this.feedbacks = feedbacks;
        this.fichas = fichas;
        this.presencas = presencas;
        this.barramento = barramento;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public Respostas.FeedbackResumo registrar(Long alunoId, Comandos.RegistrarFeedback comando) {
        LocalDate hoje = relogio.hoje();
        if (!presencas.existe(alunoId, hoje)) {
            throw new RegraDeNegocioException("Faca o check-in de hoje antes de registrar o feedback");
        }
        if (feedbacks.existe(alunoId, hoje)) {
            throw new RegraDeNegocioException("O feedback do treino de hoje ja foi registrado");
        }
        FichaDeTreino ficha = fichas.porAluno(alunoId).stream().findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("Voce ainda nao tem ficha de treino"));
        int total = ficha.itens().size();
        int concluidos = total - ficha.totalPendentes();
        int duracao = comando.duracaoMinutos() == null ? FeedbackDeTreino.DURACAO_PADRAO : comando.duracaoMinutos();
        FeedbackDeTreino novo = new FeedbackDeTreino(null, alunoId, ficha.id(), hoje, comando.esforco(),
                comando.observacao(), concluidos, total, duracao, Boolean.TRUE.equals(comando.dor()),
                comando.localDaDor(), null);

        List<FeedbackDeTreino> historico = feedbacks.porAluno(alunoId);
        AnaliseDeCarga.Resultado analise = AnaliseDeCarga.analisar(historico, novo, hoje);
        FeedbackDeTreino salvo = feedbacks.salvar(novo.comAlerta(analise.alerta()));

        publicador.publicar(EventoDeDominio.de(EventoDeDominio.FEEDBACK_TREINO, String.valueOf(salvo.id()),
                "Aluno " + alunoId + " avaliou o treino: esforco " + salvo.esforco() + "/10, "
                        + salvo.duracaoMinutos() + " min, carga " + salvo.carga()));
        if (salvo.alerta() != AlertaDeCarga.NENHUM) {
            publicador.publicar(EventoDeDominio.de(EventoDeDominio.ALERTA_DE_CARGA, String.valueOf(salvo.id()),
                    "Aluno " + alunoId + ": " + salvo.alerta().descricao()
                            + (analise.acwr() == null ? "" : " (ACWR " + analise.acwr() + ")")));
        }
        // Dor relatada: o contexto Atendimento reage ao evento DorRelatada e abre um chamado prioritario.
        barramento.postarTodos(salvo.eventos());
        return resumo(salvo, analise);
    }

    @Transactional(readOnly = true)
    public List<Respostas.FeedbackResumo> doAluno(Long alunoId) {
        return feedbacks.porAluno(alunoId).stream().map(f -> resumo(f, null)).toList();
    }

    @Transactional(readOnly = true)
    public boolean registradoHoje(Long alunoId) {
        return feedbacks.existe(alunoId, relogio.hoje());
    }

    static Respostas.FeedbackResumo resumo(FeedbackDeTreino f, AnaliseDeCarga.Resultado analise) {
        return new Respostas.FeedbackResumo(f.id(), f.alunoId(), f.data(), f.esforco(), f.observacao(),
                f.exerciciosConcluidos(), f.totalDeExercicios(), f.duracaoMinutos(), f.carga(), f.dor(),
                f.localDaDor(), f.alerta().name(), f.alerta().descricao(),
                analise == null ? null : analise.cargaAguda(), analise == null ? null : analise.acwr());
    }
}
