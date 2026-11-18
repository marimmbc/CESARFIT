package school.cesar.gymflow.infrastructure.persistence.adapter;

import org.springframework.stereotype.Repository;

import school.cesar.gymflow.domain.atendimento.repositorio.RepositorioDeEquipamentosIndisponiveis;
import school.cesar.gymflow.infrastructure.persistence.entity.EquipamentoIndisponivelEntity;
import school.cesar.gymflow.infrastructure.persistence.springdata.EquipamentoIndisponivelJpaRepository;

/** Adaptador de saida da visao de equipamentos indisponiveis. */
@Repository
public class EquipamentoIndisponivelRepositorioJpa implements RepositorioDeEquipamentosIndisponiveis {

    private final EquipamentoIndisponivelJpaRepository jpa;

    public EquipamentoIndisponivelRepositorioJpa(EquipamentoIndisponivelJpaRepository jpa) {
        this.jpa = jpa;
    }

    private static String chave(String equipamento) {
        return equipamento.trim().toLowerCase();
    }

    @Override
    public void marcarIndisponivel(String equipamento) {
        EquipamentoIndisponivelEntity e = new EquipamentoIndisponivelEntity();
        e.equipamento = chave(equipamento);
        jpa.save(e);
    }

    @Override
    public void liberar(String equipamento) {
        jpa.deleteById(chave(equipamento));
    }

    @Override
    public boolean indisponivel(String equipamento) {
        return jpa.existsById(chave(equipamento));
    }
}
