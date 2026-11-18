package school.cesar.gymflow.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.matricula.model.Matricula;
import school.cesar.gymflow.domain.matricula.model.MotivoDeEstorno;
import school.cesar.gymflow.domain.matricula.model.Pagamento;
import school.cesar.gymflow.domain.matricula.model.StatusMatricula;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeMatriculas;
import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;

/**
 * Funcionalidade: estorno de pagamento (total ou parcial, com categoria, prazo de 90 dias,
 * regra de duplicidade e direito de arrependimento que cancela a matricula sem taxa).
 */
@Service
public class EstornarPagamentoUseCase {

    private final RepositorioDeMatriculas matriculas;
    private final NomesDeAlunos nomes;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;

    public EstornarPagamentoUseCase(RepositorioDeMatriculas matriculas, NomesDeAlunos nomes,
                                    PublicadorDeEventos publicador, Relogio relogio) {
        this.matriculas = matriculas;
        this.nomes = nomes;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    /** Estorno integral, categoria OUTRO. */
    @Transactional
    public Respostas.MatriculaResumo executar(Long matriculaId, int indiceDoPagamento, String motivo) {
        return executar(matriculaId, indiceDoPagamento, new Comandos.EstornarPagamento(null, null, motivo));
    }

    @Transactional
    public Respostas.MatriculaResumo executar(Long matriculaId, int indiceDoPagamento, Comandos.EstornarPagamento comando) {
        Matricula matricula = matriculas.porId(matriculaId)
                .orElseThrow(() -> new RegraDeNegocioException("Matricula nao encontrada"));
        boolean estavaCancelada = matricula.status() == StatusMatricula.CANCELADA;
        Dinheiro valor = comando.valor() == null || comando.valor().isBlank() ? null : Dinheiro.de(comando.valor());
        MotivoDeEstorno categoria = MotivoDeEstorno.de(comando.categoria());
        if (categoria == MotivoDeEstorno.CONTESTACAO_PROCEDENTE) {
            throw new RegraDeNegocioException("Esta categoria e exclusiva da analise de contestacoes");
        }
        Pagamento estornado = matricula.estornarPagamento(indiceDoPagamento, valor, categoria, comando.motivo(),
                relogio.hoje());
        Matricula salva = matriculas.salvar(matricula);
        publicador.publicar(EventoDeDominio.de(EventoDeDominio.PAGAMENTO_ESTORNADO, String.valueOf(salva.id()),
                "Estorno " + (estornado.estornado() ? "integral" : "parcial") + " [" + categoria + "] de "
                        + (valor == null ? "saldo" : valor) + " (" + estornado.formaDePagamento() + "): "
                        + estornado.motivoDoEstorno() + ". Total pago agora: " + salva.totalPago()));
        if (!estavaCancelada && salva.status() == StatusMatricula.CANCELADA) {
            publicador.publicar(EventoDeDominio.de(EventoDeDominio.MATRICULA_CANCELADA, String.valueOf(salva.id()),
                    "Cancelada por desistencia no prazo de arrependimento, sem taxa"));
        }
        return Resumos.matricula(salva, nomes.de(salva.alunoId()));
    }
}
