package school.cesar.gymflow.infrastructure.transacao;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import school.cesar.gymflow.application.Transacional;

/**
 * Padrao Proxy: fica na frente do caso de uso, com a mesma interface publica, e controla o acesso
 * aos metodos marcados com {@link Transacional} abrindo, confirmando ou desfazendo a transacao.
 * O caso de uso continua sendo Java puro; so a infraestrutura conhece o gerenciador de transacoes.
 */
public class ProxyTransacional implements MethodInterceptor {

    private final PlatformTransactionManager gerenciador;

    public ProxyTransacional(PlatformTransactionManager gerenciador) {
        this.gerenciador = gerenciador;
    }

    @Override
    public Object invoke(MethodInvocation chamada) throws Throwable {
        Transacional transacional = transacionalDe(chamada);
        if (transacional == null) {
            return chamada.proceed();
        }
        TransactionTemplate transacao = new TransactionTemplate(gerenciador);
        transacao.setReadOnly(transacional.somenteLeitura());
        try {
            return transacao.execute(status -> {
                try {
                    return chamada.proceed();
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new FalhaVerificada(e);
                }
            });
        } catch (FalhaVerificada e) {
            throw e.getCause();
        }
    }

    private static Transacional transacionalDe(MethodInvocation chamada) {
        Object alvo = chamada.getThis();
        Method metodo = alvo == null ? chamada.getMethod()
                : AopUtils.getMostSpecificMethod(chamada.getMethod(), alvo.getClass());
        return AnnotationUtils.findAnnotation(metodo, Transacional.class);
    }

    /** Transporta uma excecao verificada para fora do TransactionTemplate. */
    private static final class FalhaVerificada extends RuntimeException {
        FalhaVerificada(Throwable causa) {
            super(causa);
        }
    }
}
