package school.cesar.gymflow.application.reacao;

import java.time.LocalDate;

import school.cesar.gymflow.application.usecase.GerarFichaDeTreinoUseCase;
import school.cesar.gymflow.domain.matricula.evento.MatriculaEfetivada;
import school.cesar.gymflow.domain.shared.evento.ManipuladorDeEvento;
import school.cesar.gymflow.domain.treino.repositorio.RepositorioDeFichas;

/** Contexto Treino reage a Matricula: todo aluno novo, com matricula vigente, recebe a ficha padrao A/B/C. */
public class GerarFichaAoEfetivarMatricula implements ManipuladorDeEvento<MatriculaEfetivada> {

    private final RepositorioDeFichas fichas;
    private final GerarFichaDeTreinoUseCase gerarFicha;

    public GerarFichaAoEfetivarMatricula(RepositorioDeFichas fichas, GerarFichaDeTreinoUseCase gerarFicha) {
        this.fichas = fichas;
        this.gerarFicha = gerarFicha;
    }

    @Override
    public Class<MatriculaEfetivada> tipo() {
        return MatriculaEfetivada.class;
    }

    @Override
    public void tratar(MatriculaEfetivada evento) {
        LocalDate hoje = LocalDate.now();
        boolean vigente = !evento.inicio().isAfter(hoje) && !evento.fim().isBefore(hoje);
        if (vigente && fichas.porAluno(evento.alunoId()).isEmpty()) {
            gerarFicha.executar(evento.alunoId());
        }
    }
}
