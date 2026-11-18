package school.cesar.gymflow.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Entidade interna do agregado Matricula mapeada como colecao de elementos. */
@Embeddable
public class PagamentoEmbeddable {

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate data;

    @Column(name = "forma_pagamento", nullable = false)
    private String formaDePagamento;

    @Column(name = "estornado")
    private Boolean estornado;

    @Column(name = "motivo_estorno", length = 300)
    private String motivoDoEstorno;

    @Column(name = "data_estorno")
    private LocalDate dataDoEstorno;

    @Column(name = "valor_estornado", precision = 10, scale = 2)
    private BigDecimal valorEstornado;

    @Column(name = "categoria_estorno", length = 30)
    private String categoriaDoEstorno;

    @Column(name = "multa", precision = 10, scale = 2)
    private BigDecimal multa;

    @Column(name = "juros", precision = 10, scale = 2)
    private BigDecimal juros;

    @Column(name = "parcelas")
    private Integer parcelas;

    @Column(name = "troco", precision = 10, scale = 2)
    private BigDecimal troco;

    public void definirEncargos(BigDecimal multa, BigDecimal juros, int parcelas, BigDecimal troco) {
        this.multa = multa;
        this.juros = juros;
        this.parcelas = parcelas;
        this.troco = troco;
    }

    public BigDecimal getMulta() { return multa; }
    public BigDecimal getJuros() { return juros; }
    public int getParcelas() { return parcelas == null ? 1 : parcelas; }
    public BigDecimal getTroco() { return troco; }

    protected PagamentoEmbeddable() {
    }

    public PagamentoEmbeddable(BigDecimal valor, LocalDate data, String formaDePagamento) {
        this.valor = valor;
        this.data = data;
        this.formaDePagamento = formaDePagamento;
    }

    public PagamentoEmbeddable(BigDecimal valor, LocalDate data, String formaDePagamento,
                               boolean estornado, String motivoDoEstorno, LocalDate dataDoEstorno,
                               BigDecimal valorEstornado, String categoriaDoEstorno) {
        this(valor, data, formaDePagamento);
        this.estornado = estornado;
        this.motivoDoEstorno = motivoDoEstorno;
        this.dataDoEstorno = dataDoEstorno;
        this.valorEstornado = valorEstornado;
        this.categoriaDoEstorno = categoriaDoEstorno;
    }

    public BigDecimal getValorEstornado() {
        return valorEstornado;
    }

    public String getCategoriaDoEstorno() {
        return categoriaDoEstorno;
    }

    public boolean isEstornado() {
        return Boolean.TRUE.equals(estornado);
    }

    public String getMotivoDoEstorno() {
        return motivoDoEstorno;
    }

    public LocalDate getDataDoEstorno() {
        return dataDoEstorno;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public String getFormaDePagamento() {
        return formaDePagamento;
    }
}
