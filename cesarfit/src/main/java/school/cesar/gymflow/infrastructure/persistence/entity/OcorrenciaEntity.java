package school.cesar.gymflow.infrastructure.persistence.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import school.cesar.gymflow.domain.operacao.model.Severidade;
import school.cesar.gymflow.domain.operacao.model.StatusOcorrencia;

/** Mapeamento objeto-relacional (campos publicos: estrutura de dados da camada de infraestrutura). */
@Entity
@Table(name = "ocorrencia_equipamento")
public class OcorrenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 80)
    public String equipamento;

    @Column(nullable = false, length = 500)
    public String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public StatusOcorrencia status;

    @Column(name = "aberta_em", nullable = false)
    public LocalDateTime abertaEm;

    @Column(name = "atualizada_em", nullable = false)
    public LocalDateTime atualizadaEm;

    @Column(length = 500)
    public String solucao;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    public Severidade severidade;

    @Column(name = "inicio_sla")
    public LocalDateTime inicioDoSla;

    @Column(name = "minutos_pausados")
    public Long minutosPausados;

    @Column(name = "pausada_desde")
    public LocalDateTime pausadaDesde;

    @Column(name = "peca_aguardada", length = 120)
    public String pecaAguardada;

    @Column(name = "custo_reparo", precision = 10, scale = 2)
    public java.math.BigDecimal custoDoReparo;

    @Column(name = "resolvida_em")
    public LocalDateTime resolvidaEm;

    @Column
    public Integer reaberturas;

    @Column
    public Boolean reincidente;
}
