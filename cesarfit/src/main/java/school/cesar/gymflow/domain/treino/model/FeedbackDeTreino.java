package school.cesar.gymflow.domain.treino.model;

import java.time.LocalDate;

import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Texto;

/**
 * Percepcao do aluno ao fim do treino: esforco (escala de Borg CR-10), duracao, dor e observacoes.
 * A carga interna do treino e calculada pelo metodo sRPE (Foster): esforco x duracao em minutos.
 */
public record FeedbackDeTreino(Long id, Long alunoId, Long fichaId, LocalDate data, int esforco,
                               String observacao, int exerciciosConcluidos, int totalDeExercicios,
                               int duracaoMinutos, boolean dor, String localDaDor, AlertaDeCarga alerta) {

    public static final int DURACAO_MINIMA = 10;
    public static final int DURACAO_MAXIMA = 240;
    public static final int DURACAO_PADRAO = 60;

    public FeedbackDeTreino {
        if (alunoId == null || fichaId == null) {
            throw new RegraDeNegocioException("O feedback deve estar vinculado ao aluno e a ficha");
        }
        if (esforco < 1 || esforco > 10) {
            throw new RegraDeNegocioException("A percepcao de esforco deve ser de 1 a 10");
        }
        if (exerciciosConcluidos < 1) {
            throw new RegraDeNegocioException("Conclua ao menos um exercicio antes de registrar o feedback");
        }
        if (duracaoMinutos < DURACAO_MINIMA || duracaoMinutos > DURACAO_MAXIMA) {
            throw new RegraDeNegocioException("A duracao do treino deve ser de " + DURACAO_MINIMA + " a "
                    + DURACAO_MAXIMA + " minutos");
        }
        observacao = Texto.opcional(observacao, 500, "Observacao");
        localDaDor = dor ? Texto.obrigatorio(localDaDor, 3, 80, "Local da dor") : null;
        alerta = alerta == null ? AlertaDeCarga.NENHUM : alerta;
    }

    /** Carga interna (unidades arbitrarias). */
    public int carga() {
        return esforco * duracaoMinutos;
    }

    public FeedbackDeTreino comAlerta(AlertaDeCarga novoAlerta) {
        return new FeedbackDeTreino(id, alunoId, fichaId, data, esforco, observacao, exerciciosConcluidos,
                totalDeExercicios, duracaoMinutos, dor, localDaDor, novoAlerta);
    }

    /** Eventos que este feedback gera para outros contextos (so depois de salvo, com id). */
    public java.util.List<school.cesar.gymflow.domain.shared.evento.Evento> eventos() {
        return dor && id != null
                ? java.util.List.of(new school.cesar.gymflow.domain.treino.evento.DorRelatada(id, alunoId, localDaDor))
                : java.util.List.of();
    }

    public boolean fichaCompleta() {
        return exerciciosConcluidos >= totalDeExercicios;
    }
}
