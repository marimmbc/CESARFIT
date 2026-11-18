package school.cesar.gymflow.domain.financeiro.evento;

import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.evento.Evento;

/**
 * Contexto Financeiro: uma contestacao foi julgada procedente (valor = null, estorna o saldo) ou
 * parcialmente procedente (valor informado). O contexto Matricula estorna o pagamento.
 */
public record ContestacaoJulgadaProcedente(Long contestacaoId, Long matriculaId, int indiceDoPagamento,
                                           Dinheiro valor, String resultado, String parecer) implements Evento {

    @Override
    public String referencia() {
        return String.valueOf(contestacaoId);
    }

    @Override
    public String descricao() {
        return "Contestacao " + contestacaoId + " " + resultado + ": estornar "
                + (valor == null ? "o saldo" : valor.toString()) + " do pagamento " + indiceDoPagamento
                + " da matricula " + matriculaId;
    }
}
