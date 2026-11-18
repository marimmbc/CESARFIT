package school.cesar.gymflow.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;

import school.cesar.gymflow.infrastructure.persistence.entity.EquipamentoIndisponivelEntity;

public interface EquipamentoIndisponivelJpaRepository extends JpaRepository<EquipamentoIndisponivelEntity, String> {
}
