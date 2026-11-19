package school.cesar.gymflow.application.reacao;

import school.cesar.gymflow.domain.atendimento.repositorio.RepositorioDeEquipamentosIndisponiveis;
import school.cesar.gymflow.domain.operacao.evento.EquipamentoLiberado;
import school.cesar.gymflow.domain.shared.evento.ManipuladorDeEvento;

/** Contexto Atendimento reage a Operacao: o equipamento voltou a funcionar. */
public class LiberarEquipamento implements ManipuladorDeEvento<EquipamentoLiberado> {

    private final RepositorioDeEquipamentosIndisponiveis indisponiveis;

    public LiberarEquipamento(RepositorioDeEquipamentosIndisponiveis indisponiveis) {
        this.indisponiveis = indisponiveis;
    }

    @Override
    public Class<EquipamentoLiberado> tipo() {
        return EquipamentoLiberado.class;
    }

    @Override
    public void tratar(EquipamentoLiberado evento) {
        indisponiveis.liberar(evento.equipamento());
    }
}
