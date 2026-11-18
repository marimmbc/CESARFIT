package school.cesar.gymflow.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.matricula.model.Aluno;
import school.cesar.gymflow.domain.matricula.model.FormaDePagamento;
import school.cesar.gymflow.domain.matricula.model.Matricula;
import school.cesar.gymflow.domain.matricula.model.Pagamento;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeAlunos;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeMatriculas;
import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;

/**
 * Funcionalidade (media): registro e regularizacao de pagamentos.
 * Regras em {@link Matricula#registrarPagamento}: encargos por atraso (multa 2% + juros 0,033% ao dia),
 * troco so em dinheiro, parcelamento so no credito e no plano Anual, credito de troca de plano abatido
 * e renovacao automatica da vigencia quando o pagamento e feito em atraso.
 */
@Service
public class RegistrarPagamentoUseCase {

    private final RepositorioDeMatriculas repositorioDeMatriculas;
    private final RepositorioDeAlunos repositorioDeAlunos;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;

    public RegistrarPagamentoUseCase(RepositorioDeMatriculas repositorioDeMatriculas,
                                     RepositorioDeAlunos repositorioDeAlunos,
                                     PublicadorDeEventos publicador, Relogio relogio) {
        this.repositorioDeMatriculas = repositorioDeMatriculas;
        this.repositorioDeAlunos = repositorioDeAlunos;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public Respostas.MatriculaResumo executar(Comandos.RegistrarPagamento comando) {
        Matricula matricula = repositorioDeMatriculas.porId(comando.matriculaId())
                .orElseThrow(() -> new RegraDeNegocioException("Matricula nao encontrada"));
        FormaDePagamento forma = FormaDePagamento.de(comando.formaDePagamento());
        int parcelas = comando.parcelas() == null ? 1 : comando.parcelas();
        Pagamento pago = matricula.registrarPagamento(Dinheiro.de(comando.valor()), forma, parcelas, relogio.hoje());
        Matricula salva = repositorioDeMatriculas.salvar(matricula);

        StringBuilder detalhe = new StringBuilder("Pagamento de " + pago.valor() + " via " + forma.nome());
        if (pago.parcelas() > 1) {
            detalhe.append(" em ").append(pago.parcelas()).append("x");
        }
        if (pago.comEncargos()) {
            detalhe.append(" com multa de ").append(pago.multa()).append(" e juros de ").append(pago.juros())
                    .append("; vigencia renovada ate ").append(salva.fim());
        }
        if (pago.troco().positivo()) {
            detalhe.append(" (troco de ").append(pago.troco()).append(")");
        }
        publicador.publicar(EventoDeDominio.de(EventoDeDominio.PAGAMENTO_REGISTRADO,
                String.valueOf(salva.id()), detalhe.toString()));

        String nome = repositorioDeAlunos.porId(salva.alunoId()).map(Aluno::nome).orElse("-");
        return Resumos.matricula(salva, nome);
    }
}
