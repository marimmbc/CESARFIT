package school.cesar.gymflow.application.usecase;

import school.cesar.gymflow.application.Transacional;
import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.treino.model.FichaDeTreino;
import school.cesar.gymflow.domain.treino.repositorio.RepositorioDeFichas;

/** Caso de uso: marcar item da ficha como concluido (complexidade baixa). */
public class ConcluirItemDeTreinoUseCase {

    private final RepositorioDeFichas repositorioDeFichas;

    public ConcluirItemDeTreinoUseCase(RepositorioDeFichas repositorioDeFichas) {
        this.repositorioDeFichas = repositorioDeFichas;
    }

    @Transacional
    public Respostas.FichaResumo executar(Comandos.ConcluirItem comando) {
        FichaDeTreino ficha = repositorioDeFichas.porId(comando.fichaId())
                .orElseThrow(() -> new RegraDeNegocioException("Ficha nao encontrada"));
        ficha.concluirItem(comando.posicao());
        return GerarFichaDeTreinoUseCase.montarResumo(repositorioDeFichas.salvar(ficha));
    }
}
