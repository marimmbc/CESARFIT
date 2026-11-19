package school.cesar.gymflow.application;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um metodo de caso de uso que deve rodar dentro de uma transacao.
 * E Java puro: a camada de aplicacao nao depende de framework (arquitetura limpa).
 * Quem aplica a transacao e o ProxyTransacional, na infraestrutura.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Transacional {

    /** Transacao so de leitura (consultas). */
    boolean somenteLeitura() default false;
}
