package school.cesar.gymflow.application.usecase;

import school.cesar.gymflow.application.Transacional;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.matricula.model.Matricula;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeMatriculas;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;

/**
 * Funcionalidade (parte de "Renovacao e alteracao de plano", alta): trancamento da matricula Anual.
 * Regras em {@link Matricula#trancar} e {@link Matricula#destrancar}.
 */
public class TrancamentoDeMatriculaUseCase {

    private final RepositorioDeMatriculas matriculas;
    private final NomesDeAlunos nomes;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;

    public TrancamentoDeMatriculaUseCase(RepositorioDeMatriculas matriculas, NomesDeAlunos nomes,
                                         PublicadorDeEventos publicador, Relogio relogio) {
        this.matriculas = matriculas;
        this.nomes = nomes;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transacional
    public Respostas.MatriculaResumo trancar(Long matriculaId, Integer dias) {
        Matricula m = buscar(matriculaId);
        if (dias == null) {
            throw new RegraDeNegocioException("Informe por quantos dias a matricula ficara trancada");
        }
        m.trancar(dias, relogio.hoje());
        Matricula salva = matriculas.salvar(m);
        publicador.publicar(EventoDeDominio.de(EventoDeDominio.MATRICULA_ALTERADA, String.valueOf(salva.id()),
                "Matricula trancada por " + dias + " dias (ate " + salva.trancadaAte() + "); nova vigencia ate "
                        + salva.fim()));
        return Resumos.matricula(salva, nomes.de(salva.alunoId()));
    }

    @Transacional
    public Respostas.MatriculaResumo destrancar(Long matriculaId) {
        Matricula m = buscar(matriculaId);
        int devolvidos = m.destrancar(relogio.hoje());
        Matricula salva = matriculas.salvar(m);
        publicador.publicar(EventoDeDominio.de(EventoDeDominio.MATRICULA_ALTERADA, String.valueOf(salva.id()),
                "Matricula destrancada antes do previsto: " + devolvidos + " dia(s) devolvido(s); vigencia ate "
                        + salva.fim()));
        return Resumos.matricula(salva, nomes.de(salva.alunoId()));
    }

    private Matricula buscar(Long id) {
        return matriculas.porId(id).orElseThrow(() -> new RegraDeNegocioException("Matricula nao encontrada"));
    }
}
