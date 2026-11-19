package school.cesar.gymflow.application.reacao;

import school.cesar.gymflow.domain.atendimento.repositorio.RepositorioDeEquipamentosIndisponiveis;
import school.cesar.gymflow.domain.operacao.evento.EquipamentoInterditado;
import school.cesar.gymflow.domain.shared.evento.ManipuladorDeEvento;

/** Contexto Atendimento reage a Operacao: anota o equipamento interditado na sua propria visao. */
public class MarcarEquipamentoIndisponivel implements ManipuladorDeEvento<EquipamentoInterditado> {

    private final RepositorioDeEquipamentosIndisponiveis indisponiveis;

    public MarcarEquipamentoIndisponivel(RepositorioDeEquipamentosIndisponiveis indisponiveis) {
        this.indisponiveis = indisponiveis;
    }

    @Override
    public Class<EquipamentoInterditado> tipo() {
        return EquipamentoInterditado.class;
    }

    @Override
    public void tratar(EquipamentoInterditado evento) {
        indisponiveis.marcarIndisponivel(evento.equipamento());
    }
}
