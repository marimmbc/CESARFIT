package school.cesar.gymflow.domain.atendimento.repositorio;

/**
 * Visao do contexto Atendimento sobre os equipamentos fora de uso.
 * Alimentada pelos eventos EquipamentoInterditado / EquipamentoLiberado do contexto Operacao,
 * sem que Atendimento precise ler as ocorrencias de outro contexto.
 */
public interface RepositorioDeEquipamentosIndisponiveis {

    void marcarIndisponivel(String equipamento);

    void liberar(String equipamento);

    boolean indisponivel(String equipamento);
}
