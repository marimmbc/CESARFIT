package school.cesar.gymflow.domain.matricula.model;

import java.time.LocalDate;

import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Texto;

/**
 * Objeto de valor: um pagamento da matricula.
 * "valor" e o que ficou com a academia (o troco ja descontado); multa e juros estao incluidos nele.
 * Pode receber um ou mais estornos parciais ate zerar o saldo; "estornado" = totalmente estornado.
 */
public record Pagamento(Dinheiro valor, LocalDate data, String formaDePagamento,
                        boolean estornado, String motivoDoEstorno, LocalDate dataDoEstorno,
                        Dinheiro valorEstornado, MotivoDeEstorno categoriaDoEstorno,
                        Dinheiro multa, Dinheiro juros, int parcelas, Dinheiro troco) {

    public Pagamento {
        valorEstornado = valorEstornado == null ? (estornado ? valor : Dinheiro.ZERO) : valorEstornado;
        multa = multa == null ? Dinheiro.ZERO : multa;
        juros = juros == null ? Dinheiro.ZERO : juros;
        troco = troco == null ? Dinheiro.ZERO : troco;
        parcelas = parcelas < 1 ? 1 : parcelas;
    }

    public Pagamento(Dinheiro valor, LocalDate data, String formaDePagamento) {
        this(valor, data, formaDePagamento, false, null, null, Dinheiro.ZERO, null,
                Dinheiro.ZERO, Dinheiro.ZERO, 1, Dinheiro.ZERO);
    }

    public Pagamento(Dinheiro valor, LocalDate data, String formaDePagamento, boolean estornado,
                     String motivoDoEstorno, LocalDate dataDoEstorno, Dinheiro valorEstornado,
                     MotivoDeEstorno categoriaDoEstorno) {
        this(valor, data, formaDePagamento, estornado, motivoDoEstorno, dataDoEstorno, valorEstornado,
                categoriaDoEstorno, Dinheiro.ZERO, Dinheiro.ZERO, 1, Dinheiro.ZERO);
    }

    public static Pagamento novo(Dinheiro valor, String formaDePagamento) {
        return new Pagamento(valor, LocalDate.now(), formaDePagamento);
    }

    /** Quanto ainda pode ser estornado deste pagamento. */
    public Dinheiro saldoEstornavel() {
        return valor.subtrair(valorEstornado);
    }

    public boolean parcialmenteEstornado() {
        return !estornado && valorEstornado.positivo();
    }

    public boolean comEncargos() {
        return multa.positivo() || juros.positivo();
    }

    Pagamento estornar(Dinheiro quanto, MotivoDeEstorno categoria, String motivo, LocalDate hoje) {
        if (estornado) {
            throw new RegraDeNegocioException("Pagamento ja foi estornado");
        }
        String limpo = Texto.obrigatorio(motivo, 10, 300, "Motivo do estorno");
        if (quanto == null || !quanto.positivo()) {
            throw new RegraDeNegocioException("Valor do estorno deve ser maior que zero");
        }
        if (quanto.maiorQue(saldoEstornavel())) {
            throw new RegraDeNegocioException("Valor do estorno excede o saldo estornavel de " + saldoEstornavel());
        }
        Dinheiro novoTotal = valorEstornado.somar(quanto);
        return new Pagamento(valor, data, formaDePagamento, novoTotal.equals(valor), limpo, hoje, novoTotal, categoria,
                multa, juros, parcelas, troco);
    }
}
