package school.cesar.gymflow.domain.matricula.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

/**
 * Raiz de agregado: Matricula.
 * Associa um aluno a um plano (Mensal ou Anual). O valor e SEMPRE recebido ja
 * calculado pela politica de preco do plano, tanto na abertura quanto na edicao,
 * e fica salvo junto da matricula.
 */
public class Matricula extends school.cesar.gymflow.domain.shared.evento.RaizDeAgregado {

    private Long id;
    private Long alunoId;
    private Modalidade modalidade;
    private Dinheiro valorMensal;
    private LocalDate inicio;
    private LocalDate fim;
    private StatusMatricula status;
    private final List<Pagamento> pagamentos = new ArrayList<>();
    private String justificativaDoCancelamento;
    private LocalDate dataDaSolicitacaoDeCancelamento;
    private Dinheiro taxaDeCancelamento;
    private Dinheiro creditoDeTroca = Dinheiro.ZERO;
    private LocalDate trancadaDe;
    private LocalDate trancadaAte;
    private int diasTrancados;
    private int trancamentos;

    public Matricula(Long id, Long alunoId, Modalidade modalidade, Dinheiro valorMensal,
                     LocalDate inicio, LocalDate fim, StatusMatricula status,
                     List<Pagamento> pagamentos) {
        this.id = id;
        this.alunoId = alunoId;
        this.modalidade = modalidade;
        this.valorMensal = valorMensal;
        this.inicio = inicio;
        this.fim = fim;
        this.status = status;
        if (pagamentos != null) {
            this.pagamentos.addAll(pagamentos);
        }
    }

    /** Fabrica de agregado. */
    public static Matricula abrir(Long alunoId, Modalidade modalidade, Dinheiro valor, LocalDate inicio) {
        validar(alunoId, modalidade, valor, inicio);
        return new Matricula(null, alunoId, modalidade, valor, inicio,
                modalidade.fimAPartirDe(inicio), StatusMatricula.ATIVA, List.of());
    }

    /**
     * Edicao da matricula. Ao trocar o plano, o valor recalculado e a nova
     * vigencia passam a valer imediatamente e sao persistidos.
     */
    public void alterar(Long novoAlunoId, Modalidade novaModalidade, Dinheiro novoValor,
                        LocalDate novoInicio, StatusMatricula novoStatus) {
        validar(novoAlunoId, novaModalidade, novoValor, novoInicio);
        if (novoStatus == null) {
            throw new RegraDeNegocioException("Situacao da matricula e obrigatoria");
        }
        if (novoStatus == StatusMatricula.CANCELADA && status != StatusMatricula.CANCELADA) {
            throw new RegraDeNegocioException("Para cancelar, use o cancelamento com justificativa");
        }
        if (trancadaEm(LocalDate.now()) && novaModalidade != modalidade) {
            throw new RegraDeNegocioException("Destranque a matricula antes de trocar de plano");
        }
        if (novaModalidade != modalidade) {
            diasTrancados = 0;
            trancamentos = 0;
            trancadaDe = null;
            trancadaAte = null;
        }
        this.alunoId = novoAlunoId;
        this.modalidade = novaModalidade;
        this.valorMensal = novoValor;
        this.inicio = novoInicio;
        this.fim = novaModalidade.fimAPartirDe(novoInicio);
        this.status = novoStatus;
    }

    private static void validar(Long alunoId, Modalidade modalidade, Dinheiro valor, LocalDate inicio) {
        if (alunoId == null) {
            throw new RegraDeNegocioException("Matricula exige um aluno");
        }
        if (modalidade == null) {
            throw new RegraDeNegocioException("Matricula exige um plano (Mensal ou Anual)");
        }
        if (valor == null || !valor.positivo()) {
            throw new RegraDeNegocioException("Valor da matricula invalido");
        }
        if (inicio == null) {
            throw new RegraDeNegocioException("Data de inicio e obrigatoria");
        }
        if (inicio.isBefore(LocalDate.now().minusYears(1)) || inicio.isAfter(LocalDate.now().plusYears(1))) {
            throw new RegraDeNegocioException("Data de inicio deve estar dentro de 1 ano a partir de hoje");
        }
    }

    /* ---------- cobranca: valor devido, encargos e pagamento ---------- */

    public static final BigDecimal MULTA_PERCENTUAL = new BigDecimal("2");
    public static final BigDecimal JUROS_PERCENTUAL_AO_DIA = new BigDecimal("0.033");
    public static final int DIAS_DE_TOLERANCIA_DE_PAGAMENTO = 5;

    /** Valor do plano menos o credito da troca de plano (nunca negativo). */
    public Dinheiro valorBase() {
        return valorMensal.subtrair(creditoDeTroca).naoNegativo();
    }

    public long diasDeAtraso(LocalDate hoje) {
        return hoje.isAfter(fim) ? java.time.temporal.ChronoUnit.DAYS.between(fim, hoje) : 0;
    }

    /** Multa de 2% sobre o valor base quando o pagamento acontece depois do fim da vigencia. */
    public Dinheiro multaEm(LocalDate hoje) {
        return diasDeAtraso(hoje) > 0 ? valorBase().percentual(MULTA_PERCENTUAL) : Dinheiro.ZERO;
    }

    /** Juros simples de 0,033% ao dia (cerca de 1% ao mes) sobre o valor base. */
    public Dinheiro jurosEm(LocalDate hoje) {
        long dias = diasDeAtraso(hoje);
        return dias > 0 ? valorBase().percentual(JUROS_PERCENTUAL_AO_DIA.multiply(BigDecimal.valueOf(dias)))
                : Dinheiro.ZERO;
    }

    public Dinheiro valorDevido(LocalDate hoje) {
        return valorBase().somar(multaEm(hoje)).somar(jurosEm(hoje));
    }

    /**
     * Registra o pagamento do periodo.
     * - matricula cancelada nao recebe pagamento;
     * - o valor devido inclui multa e juros se estiver em atraso;
     * - so dinheiro aceita valor acima do devido (gera troco); meios eletronicos devem ser exatos;
     * - parcelamento so no cartao de credito e so no plano Anual, em ate 12x;
     * - pagamento em atraso quita os encargos e renova a vigencia a partir do vencimento anterior.
     */
    public Pagamento registrarPagamento(Dinheiro valor, FormaDePagamento forma, int parcelas, LocalDate hoje) {
        if (status == StatusMatricula.CANCELADA) {
            throw new RegraDeNegocioException("Nao e possivel pagar uma matricula cancelada");
        }
        if (forma == null) {
            throw new RegraDeNegocioException("Informe a forma de pagamento");
        }
        int qtd = parcelas < 1 ? 1 : parcelas;
        if (qtd > 1 && !forma.parcelavel()) {
            throw new RegraDeNegocioException("Parcelamento apenas no cartao de credito");
        }
        if (qtd > 1 && modalidade != Modalidade.ANUAL) {
            throw new RegraDeNegocioException("Parcelamento disponivel apenas no plano Anual");
        }
        if (qtd > FormaDePagamento.MAX_PARCELAS) {
            throw new RegraDeNegocioException("Parcelamento em no maximo " + FormaDePagamento.MAX_PARCELAS + " vezes");
        }
        Dinheiro multa = multaEm(hoje);
        Dinheiro juros = jurosEm(hoje);
        Dinheiro devido = valorDevido(hoje);
        if (valor == null || !valor.maiorOuIgual(devido)) {
            if (multa.zero() && creditoDeTroca.zero()) {
                throw new RegraDeNegocioException("Pagamento inferior ao valor do plano: " + valorMensal);
            }
            throw new RegraDeNegocioException("Pagamento inferior ao valor devido: " + devido
                    + (multa.zero() ? "" : " (inclui multa de " + multa + " e juros de " + juros + ")"));
        }
        Dinheiro troco = valor.subtrair(devido);
        if (troco.positivo() && !forma.aceitaTroco()) {
            throw new RegraDeNegocioException("Pagamento por " + forma.nome() + " deve ser exatamente " + devido
                    + "; apenas dinheiro aceita troco");
        }
        boolean emAtraso = diasDeAtraso(hoje) > 0;
        Pagamento pagamento = new Pagamento(devido, hoje, forma.name(), false, null, null, Dinheiro.ZERO, null,
                multa, juros, qtd, troco);
        pagamentos.add(pagamento);
        if (emAtraso) {
            this.fim = modalidade.fimAPartirDe(fim);
            this.status = StatusMatricula.ATIVA;
        } else if (status == StatusMatricula.VENCIDA) {
            status = StatusMatricula.ATIVA;
        }
        return pagamento;
    }

    /** Inadimplente: passou a tolerancia de 5 dias do inicio e o pago (mais o credito) nao cobre o plano. */
    public boolean pagamentoPendente(LocalDate hoje) {
        if (status == StatusMatricula.CANCELADA) {
            return false;
        }
        boolean passouTolerancia = hoje.isAfter(inicio.plusDays(DIAS_DE_TOLERANCIA_DE_PAGAMENTO));
        return passouTolerancia && !totalPago().somar(creditoDeTroca).maiorOuIgual(valorMensal);
    }

    /** Pro-rata da troca de plano: o que foi pago e nao usado vira credito no novo plano. */
    public Dinheiro creditoProRata(LocalDate hoje) {
        long totalDias = java.time.temporal.ChronoUnit.DAYS.between(inicio, fim);
        long restantes = hoje.isBefore(inicio) ? totalDias
                : Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(hoje, fim));
        return totalPago().proporcao(restantes, totalDias);
    }

    public void definirCreditoDeTroca(Dinheiro credito) {
        this.creditoDeTroca = credito == null ? Dinheiro.ZERO : credito.naoNegativo();
    }

    /* ---------- trancamento (congelamento) ---------- */

    public static final int DIAS_MINIMOS_DE_TRANCAMENTO = 7;
    public static final int DIAS_MAXIMOS_DE_TRANCAMENTO_POR_CONTRATO = 60;
    public static final int MAX_TRANCAMENTOS_POR_CONTRATO = 2;

    /**
     * Congela a matricula por alguns dias, empurrando o fim da vigencia.
     * Regras: so plano Anual; matricula ativa, vigente e sem pagamento pendente; nao pode estar trancada;
     * minimo de 7 dias; no maximo 60 dias e 2 trancamentos por contrato.
     */
    public void trancar(int dias, LocalDate hoje) {
        if (modalidade != Modalidade.ANUAL) {
            throw new RegraDeNegocioException("Trancamento disponivel apenas no plano Anual");
        }
        if (status != StatusMatricula.ATIVA || hoje.isAfter(fim)) {
            throw new RegraDeNegocioException("Somente matricula ativa e vigente pode ser trancada");
        }
        if (trancadaEm(hoje)) {
            throw new RegraDeNegocioException("A matricula ja esta trancada ate "
                    + trancadaAte.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        if (pagamentoPendente(hoje)) {
            throw new RegraDeNegocioException("Regularize o pagamento antes de trancar a matricula");
        }
        if (trancamentos >= MAX_TRANCAMENTOS_POR_CONTRATO) {
            throw new RegraDeNegocioException("Limite de " + MAX_TRANCAMENTOS_POR_CONTRATO
                    + " trancamentos por contrato atingido");
        }
        if (dias < DIAS_MINIMOS_DE_TRANCAMENTO) {
            throw new RegraDeNegocioException("O trancamento minimo e de " + DIAS_MINIMOS_DE_TRANCAMENTO + " dias");
        }
        int restantes = DIAS_MAXIMOS_DE_TRANCAMENTO_POR_CONTRATO - diasTrancados;
        if (dias > restantes) {
            throw new RegraDeNegocioException("Limite de " + DIAS_MAXIMOS_DE_TRANCAMENTO_POR_CONTRATO
                    + " dias de trancamento por contrato: restam " + restantes + " dias");
        }
        trancadaDe = hoje;
        trancadaAte = hoje.plusDays(dias);
        diasTrancados += dias;
        trancamentos++;
        fim = fim.plusDays(dias);
    }

    /** Volta antes do previsto: os dias nao usados voltam para o saldo e saem do fim da vigencia. */
    public int destrancar(LocalDate hoje) {
        if (!trancadaEm(hoje)) {
            throw new RegraDeNegocioException("A matricula nao esta trancada");
        }
        int naoUsados = (int) java.time.temporal.ChronoUnit.DAYS.between(hoje, trancadaAte);
        diasTrancados -= naoUsados;
        fim = fim.minusDays(naoUsados);
        trancadaAte = hoje;
        return naoUsados;
    }

    public boolean trancadaEm(LocalDate data) {
        return trancadaDe != null && trancadaAte != null && !data.isBefore(trancadaDe) && data.isBefore(trancadaAte);
    }

    /** Usado pela persistencia. */
    public void restaurarCobranca(Dinheiro credito, LocalDate de, LocalDate ate, int dias, int quantidade) {
        this.creditoDeTroca = credito == null ? Dinheiro.ZERO : credito;
        this.trancadaDe = de;
        this.trancadaAte = ate;
        this.diasTrancados = dias;
        this.trancamentos = quantidade;
    }

    public Dinheiro creditoDeTroca() { return creditoDeTroca; }
    public LocalDate trancadaDe() { return trancadaDe; }
    public LocalDate trancadaAte() { return trancadaAte; }
    public int diasTrancados() { return diasTrancados; }
    public int trancamentos() { return trancamentos; }

    /** Renova pelo mesmo plano contratado (mais 7 dias ou mais 1 mes). */
    public void renovar() {
        if (status == StatusMatricula.CANCELADA) {
            throw new RegraDeNegocioException("Matricula cancelada nao pode ser renovada");
        }
        LocalDate base = fim.isBefore(LocalDate.now()) ? LocalDate.now() : fim;
        this.fim = modalidade.fimAPartirDe(base);
        this.status = StatusMatricula.ATIVA;
    }

    /** Cancelamento exige justificativa e registra a data da solicitacao. */
    public void cancelar(String justificativa, LocalDate dataDaSolicitacao) {
        if (status == StatusMatricula.CANCELADA) {
            throw new RegraDeNegocioException("Matricula ja esta cancelada");
        }
        String limpa = school.cesar.gymflow.domain.shared.Texto.obrigatorio(
                justificativa, 10, 500, "Justificativa do cancelamento");
        if (dataDaSolicitacao == null || dataDaSolicitacao.isAfter(LocalDate.now())) {
            throw new RegraDeNegocioException("Data da solicitacao de cancelamento invalida");
        }
        this.justificativaDoCancelamento = limpa;
        this.dataDaSolicitacaoDeCancelamento = dataDaSolicitacao;
        this.taxaDeCancelamento = taxaPara(dataDaSolicitacao);
        this.status = StatusMatricula.CANCELADA;
    }

    /**
     * Aviso previo: pedido feito com menos de 30 dias de antecedencia do fim do contrato
     * gera taxa de UMA mensalidade (Mensal = R$ 120,00; Anual = R$ 1320,00 / 12 = R$ 110,00).
     */
    public Dinheiro taxaPara(LocalDate dataDaSolicitacao) {
        long diasAteOFim = java.time.temporal.ChronoUnit.DAYS.between(dataDaSolicitacao, fim);
        if (diasAteOFim >= DIAS_DE_AVISO_PREVIO) {
            return Dinheiro.ZERO;
        }
        return Dinheiro.de(valorMensal.valor().divide(java.math.BigDecimal.valueOf(modalidade.meses()),
                2, java.math.RoundingMode.HALF_UP));
    }

    public static final int DIAS_DE_AVISO_PREVIO = 30;

    /** Usado pela persistencia para restaurar os dados do cancelamento. */
    public void restaurarCancelamento(String justificativa, LocalDate dataDaSolicitacao, Dinheiro taxa) {
        this.justificativaDoCancelamento = justificativa;
        this.dataDaSolicitacaoDeCancelamento = dataDaSolicitacao;
        this.taxaDeCancelamento = taxa;
    }

    public Dinheiro taxaDeCancelamento() {
        return taxaDeCancelamento;
    }

    public static final int DIAS_PARA_ESTORNO = 90;
    public static final int DIAS_DE_ARREPENDIMENTO = 7;
    public static final int DIAS_PARA_DUPLICIDADE = 3;

    /** Estorno integral do saldo, categoria OUTRO (compatibilidade). */
    public Pagamento estornarPagamento(int indice, String motivo) {
        return estornarPagamento(indice, null, MotivoDeEstorno.OUTRO, motivo, LocalDate.now());
    }

    /**
     * Estorna total ou parcialmente o pagamento na posicao informada.
     * - prazo: ate 90 dias da data do pagamento (exceto quando decorre de contestacao procedente);
     * - valor nulo = saldo restante; nunca acima do saldo estornavel;
     * - COBRANCA_DUPLICADA: exige outro pagamento ativo de mesmo valor em ate 3 dias e e integral;
     * - DESISTENCIA: ate 7 dias do pagamento, integral, e cancela a matricula sem taxa.
     */
    public Pagamento estornarPagamento(int indice, Dinheiro quanto, MotivoDeEstorno categoria, String motivo,
                                       LocalDate hoje) {
        Pagamento atual = pagamento(indice);
        MotivoDeEstorno tipo = categoria == null ? MotivoDeEstorno.OUTRO : categoria;
        long dias = java.time.temporal.ChronoUnit.DAYS.between(atual.data(), hoje);
        if (tipo != MotivoDeEstorno.CONTESTACAO_PROCEDENTE && dias > DIAS_PARA_ESTORNO) {
            throw new RegraDeNegocioException("Prazo de " + DIAS_PARA_ESTORNO + " dias para estorno expirado");
        }
        Dinheiro valorDoEstorno = quanto == null ? atual.saldoEstornavel() : quanto;
        boolean integral = valorDoEstorno.equals(atual.valor()) && !atual.parcialmenteEstornado();
        switch (tipo) {
            case DESISTENCIA -> {
                if (dias > DIAS_DE_ARREPENDIMENTO) {
                    throw new RegraDeNegocioException("Desistencia so pode ser estornada em ate "
                            + DIAS_DE_ARREPENDIMENTO + " dias do pagamento");
                }
                if (!integral) {
                    throw new RegraDeNegocioException("Estorno por desistencia deve ser integral");
                }
            }
            case COBRANCA_DUPLICADA -> {
                if (!integral) {
                    throw new RegraDeNegocioException("Estorno por duplicidade deve ser integral");
                }
                boolean haGemeo = false;
                for (int i = 0; i < pagamentos.size(); i++) {
                    Pagamento outro = pagamentos.get(i);
                    if (i != indice && !outro.estornado() && outro.valor().equals(atual.valor())
                            && Math.abs(java.time.temporal.ChronoUnit.DAYS.between(outro.data(), atual.data()))
                            <= DIAS_PARA_DUPLICIDADE) {
                        haGemeo = true;
                    }
                }
                if (!haGemeo) {
                    throw new RegraDeNegocioException("Nao ha outro pagamento de mesmo valor em ate "
                            + DIAS_PARA_DUPLICIDADE + " dias para caracterizar duplicidade");
                }
            }
            default -> { }
        }
        Pagamento estornado = atual.estornar(valorDoEstorno, tipo, motivo, hoje);
        pagamentos.set(indice, estornado);
        if (tipo == MotivoDeEstorno.DESISTENCIA && status != StatusMatricula.CANCELADA) {
            this.justificativaDoCancelamento = "Desistencia no prazo de arrependimento: " + estornado.motivoDoEstorno();
            this.dataDaSolicitacaoDeCancelamento = hoje;
            this.taxaDeCancelamento = Dinheiro.ZERO;
            this.status = StatusMatricula.CANCELADA;
        }
        return estornado;
    }

    public Pagamento pagamento(int indice) {
        if (indice < 0 || indice >= pagamentos.size()) {
            throw new RegraDeNegocioException("Pagamento nao encontrado nesta matricula");
        }
        return pagamentos.get(indice);
    }

    public String justificativaDoCancelamento() {
        return justificativaDoCancelamento;
    }

    public LocalDate dataDaSolicitacaoDeCancelamento() {
        return dataDaSolicitacaoDeCancelamento;
    }

    public boolean estaVigenteEm(LocalDate data) {
        return status == StatusMatricula.ATIVA
                && !data.isBefore(inicio)
                && !data.isAfter(fim);
    }

    /** Uma matricula "ocupa" o aluno enquanto nao estiver cancelada nem vencida. */
    public boolean ativa() {
        return status == StatusMatricula.ATIVA && !LocalDate.now().isAfter(fim);
    }

    public void atualizarVigencia(LocalDate hoje) {
        if (status == StatusMatricula.ATIVA && hoje.isAfter(fim)) {
            status = StatusMatricula.VENCIDA;
        }
    }

    public Dinheiro totalPago() {
        return pagamentos.stream().map(Pagamento::saldoEstornavel).reduce(Dinheiro.ZERO, Dinheiro::somar);
    }

    public void atribuirId(Long id) {
        this.id = id;
    }

    /** Evento publicado depois que a matricula nova foi salva (ja com id). */
    public school.cesar.gymflow.domain.matricula.evento.MatriculaEfetivada efetivacao() {
        if (id == null) {
            throw new RegraDeNegocioException("Matricula ainda nao foi salva");
        }
        return new school.cesar.gymflow.domain.matricula.evento.MatriculaEfetivada(id, alunoId, modalidade.nome(), inicio, fim);
    }

    public Long id() {
        return id;
    }

    public Long alunoId() {
        return alunoId;
    }

    public Modalidade modalidade() {
        return modalidade;
    }

    public String descricaoDoPlano() {
        return modalidade.nome();
    }

    public Dinheiro valorMensal() {
        return valorMensal;
    }

    public LocalDate inicio() {
        return inicio;
    }

    public LocalDate fim() {
        return fim;
    }

    public StatusMatricula status() {
        return status;
    }

    public List<Pagamento> pagamentos() {
        return Collections.unmodifiableList(pagamentos);
    }
}
