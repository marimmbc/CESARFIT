package school.cesar.gymflow.domain.treino.servico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import school.cesar.gymflow.domain.treino.model.AlertaDeCarga;
import school.cesar.gymflow.domain.treino.model.FeedbackDeTreino;

/**
 * Servico de dominio: analisa a carga de treino do aluno a partir do historico de feedbacks.
 *
 * - Carga aguda: soma das cargas dos ultimos 7 dias.
 * - Carga cronica: media semanal das cargas dos ultimos 28 dias (soma / 4).
 * - ACWR (acute:chronic workload ratio) = aguda / cronica; acima de 1,5 indica risco de lesao.
 *   So e calculado com pelo menos {@link #MINIMO_PARA_ACWR} registros em 28 dias.
 * Prioridade dos alertas: dor > risco de lesao > sobrecarga > progressao sugerida.
 */
public final class AnaliseDeCarga {

    public static final int MINIMO_PARA_ACWR = 4;
    public static final double LIMITE_ACWR = 1.5;
    public static final int TREINOS_PARA_TENDENCIA = 3;

    private AnaliseDeCarga() {
    }

    public record Resultado(AlertaDeCarga alerta, int cargaAguda, BigDecimal cargaCronica, BigDecimal acwr) {
    }

    public static Resultado analisar(List<FeedbackDeTreino> anteriores, FeedbackDeTreino atual, LocalDate hoje) {
        List<FeedbackDeTreino> todos = new ArrayList<>(anteriores);
        todos.add(atual);
        todos.sort(Comparator.comparing(FeedbackDeTreino::data).reversed());

        int aguda = soma(todos, hoje.minusDays(6), hoje);
        List<FeedbackDeTreino> mes = todos.stream().filter(f -> !f.data().isBefore(hoje.minusDays(27))).toList();
        BigDecimal cronica = BigDecimal.valueOf(soma(mes, hoje.minusDays(27), hoje)).divide(BigDecimal.valueOf(4), 2,
                RoundingMode.HALF_UP);
        BigDecimal acwr = mes.size() >= MINIMO_PARA_ACWR && cronica.signum() > 0
                ? BigDecimal.valueOf(aguda).divide(cronica, 2, RoundingMode.HALF_UP) : null;

        List<FeedbackDeTreino> ultimos = todos.stream().limit(TREINOS_PARA_TENDENCIA).toList();
        boolean tendenciaCompleta = ultimos.size() == TREINOS_PARA_TENDENCIA;

        AlertaDeCarga alerta;
        if (atual.dor()) {
            alerta = AlertaDeCarga.DOR_RELATADA;
        } else if (acwr != null && acwr.doubleValue() > LIMITE_ACWR) {
            alerta = AlertaDeCarga.RISCO_DE_LESAO;
        } else if (tendenciaCompleta && ultimos.stream().allMatch(f -> f.esforco() >= 9)) {
            alerta = AlertaDeCarga.SOBRECARGA;
        } else if (tendenciaCompleta && atual.fichaCompleta() && ultimos.stream().allMatch(f -> f.esforco() <= 3)) {
            alerta = AlertaDeCarga.PROGRESSAO_SUGERIDA;
        } else {
            alerta = AlertaDeCarga.NENHUM;
        }
        return new Resultado(alerta, aguda, cronica, acwr);
    }

    private static int soma(List<FeedbackDeTreino> lista, LocalDate de, LocalDate ate) {
        return lista.stream().filter(f -> !f.data().isBefore(de) && !f.data().isAfter(ate))
                .mapToInt(FeedbackDeTreino::carga).sum();
    }
}
