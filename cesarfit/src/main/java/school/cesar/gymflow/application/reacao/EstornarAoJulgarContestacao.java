package school.cesar.gymflow.application.reacao;

import school.cesar.gymflow.domain.financeiro.evento.ContestacaoJulgadaProcedente;
import school.cesar.gymflow.domain.matricula.model.Matricula;
import school.cesar.gymflow.domain.matricula.model.MotivoDeEstorno;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeMatriculas;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;
import school.cesar.gymflow.domain.shared.evento.ManipuladorDeEvento;

/** Contexto Matricula reage a Financeiro: contestacao procedente estorna o pagamento contestado. */
public class EstornarAoJulgarContestacao implements ManipuladorDeEvento<ContestacaoJulgadaProcedente> {

    private final RepositorioDeMatriculas matriculas;
    private final Relogio relogio;

    public EstornarAoJulgarContestacao(RepositorioDeMatriculas matriculas, Relogio relogio) {
        this.matriculas = matriculas;
        this.relogio = relogio;
    }

    @Override
    public Class<ContestacaoJulgadaProcedente> tipo() {
        return ContestacaoJulgadaProcedente.class;
    }

    @Override
    public void tratar(ContestacaoJulgadaProcedente evento) {
        Matricula matricula = matriculas.porId(evento.matriculaId())
                .orElseThrow(() -> new RegraDeNegocioException("Matricula nao encontrada"));
        String motivo = "Contestacao #" + evento.contestacaoId() + " " + evento.resultado() + ": " + evento.parecer();
        matricula.estornarPagamento(evento.indiceDoPagamento(), evento.valor(), MotivoDeEstorno.CONTESTACAO_PROCEDENTE,
                motivo.length() > 300 ? motivo.substring(0, 300) : motivo, relogio.hoje());
        matriculas.salvar(matricula);
    }
}
