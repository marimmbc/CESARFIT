package school.cesar.gymflow.infrastructure.transacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import school.cesar.gymflow.application.Transacional;

class ProxyTransacionalTest {

    /** Gerenciador falso que so conta inicios, confirmacoes e desfazimentos. */
    static class GerenciadorFalso extends AbstractPlatformTransactionManager {
        int confirmadas;
        int desfeitas;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transacao, TransactionDefinition definicao) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            confirmadas++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            desfeitas++;
        }
    }

    /** Caso de uso de exemplo: Java puro, so com a anotacao da camada de aplicacao. */
    static class CasoDeUsoDeExemplo {
        @Transacional
        public boolean gravar() {
            return TransactionSynchronizationManager.isActualTransactionActive();
        }

        @Transacional(somenteLeitura = true)
        public boolean consultar() {
            return TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        }

        @Transacional
        public void falhar() {
            throw new IllegalStateException("regra violada");
        }

        public boolean semTransacao() {
            return TransactionSynchronizationManager.isActualTransactionActive();
        }
    }

    private final GerenciadorFalso gerenciador = new GerenciadorFalso();

    private CasoDeUsoDeExemplo comProxy() {
        ProxyFactory fabrica = new ProxyFactory(new CasoDeUsoDeExemplo());
        fabrica.setProxyTargetClass(true);
        fabrica.addAdvice(new ProxyTransacional(gerenciador));
        return (CasoDeUsoDeExemplo) fabrica.getProxy();
    }

    @Test
    void metodoTransacionalRodaDentroDeTransacaoEConfirma() {
        assertTrue(comProxy().gravar());
        assertEquals(1, gerenciador.confirmadas);
    }

    @Test
    void consultaRodaEmTransacaoSomenteLeitura() {
        assertTrue(comProxy().consultar());
    }

    @Test
    void erroDesfazATransacaoEPropagaAExcecao() {
        assertThrows(IllegalStateException.class, () -> comProxy().falhar());
        assertEquals(1, gerenciador.desfeitas);
        assertEquals(0, gerenciador.confirmadas);
    }

    @Test
    void metodoSemAnotacaoNaoAbreTransacao() {
        assertFalse(comProxy().semTransacao());
        assertEquals(0, gerenciador.confirmadas);
    }
}
