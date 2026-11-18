package school.cesar.gymflow.domain.operacao.evento;

import school.cesar.gymflow.domain.shared.evento.Evento;

/** Contexto Operacao: uma ocorrencia ALTA ou CRITICA tirou o equipamento de uso. */
public record EquipamentoInterditado(Long ocorrenciaId, String equipamento, String severidade) implements Evento {

    @Override
    public String referencia() {
        return equipamento;
    }

    @Override
    public String descricao() {
        return "Equipamento " + equipamento + " interditado (" + severidade + ")";
    }
}
