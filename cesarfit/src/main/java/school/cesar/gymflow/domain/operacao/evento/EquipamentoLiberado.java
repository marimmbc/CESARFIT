package school.cesar.gymflow.domain.operacao.evento;

import school.cesar.gymflow.domain.shared.evento.Evento;

/** Contexto Operacao: o equipamento interditado voltou a funcionar. */
public record EquipamentoLiberado(Long ocorrenciaId, String equipamento) implements Evento {

    @Override
    public String referencia() {
        return equipamento;
    }

    @Override
    public String descricao() {
        return "Equipamento " + equipamento + " liberado para uso";
    }
}
