package school.cesar.gymflow.domain.treino.model;

/** Resultado da analise de carga do feedback, do mais grave para o mais brando. */
public enum AlertaDeCarga {
    DOR_RELATADA("Dor relatada: instrutor acionado com prioridade"),
    RISCO_DE_LESAO("Carga da semana muito acima da media do mes (ACWR > 1,5)"),
    SOBRECARGA("Tres treinos seguidos com esforco 9 ou 10"),
    PROGRESSAO_SUGERIDA("Treinos leves com ficha completa: hora de aumentar a carga"),
    NENHUM("Carga adequada");

    private final String descricao;

    AlertaDeCarga(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
