package school.cesar.gymflow.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Visao de leitura do contexto Atendimento: equipamentos interditados (chave = nome em minusculas). */
@Entity
@Table(name = "equipamento_indisponivel")
public class EquipamentoIndisponivelEntity {

    @Id
    @Column(length = 80)
    public String equipamento;
}
