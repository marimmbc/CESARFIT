package school.cesar.gymflow.infrastructure.config;

import java.util.Arrays;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.transaction.PlatformTransactionManager;

import school.cesar.gymflow.application.Transacional;
import school.cesar.gymflow.infrastructure.transacao.ProxyTransacional;

/**
 * A camada de aplicacao e Java puro (sem @Service, @Component ou @Transactional).
 * Aqui, na infraestrutura, os casos de uso e as reacoes a eventos sao registrados como beans,
 * e cada caso de uso com metodos {@link Transacional} e envolvido pelo {@link ProxyTransacional}.
 */
@Configuration
@ComponentScan(
        basePackages = "school.cesar.gymflow.application",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                pattern = "school\\.cesar\\.gymflow\\.application\\.(usecase|reacao)\\.[A-Za-z]+"),
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                pattern = "school\\.cesar\\.gymflow\\.application\\.usecase\\.Resumos"))
public class BeansDeAplicacao {

    private static final String PACOTE_DA_APLICACAO = "school.cesar.gymflow.application.";

    @Bean
    public static BeanPostProcessor aplicadorDeTransacoes(ObjectProvider<PlatformTransactionManager> gerenciador) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nome) {
                Class<?> tipo = bean.getClass();
                if (!tipo.getName().startsWith(PACOTE_DA_APLICACAO) || !temMetodoTransacional(tipo)) {
                    return bean;
                }
                ProxyFactory fabrica = new ProxyFactory(bean);
                fabrica.setProxyTargetClass(true);
                fabrica.addAdvice(new ProxyTransacional(gerenciador.getObject()));
                return fabrica.getProxy(tipo.getClassLoader());
            }
        };
    }

    private static boolean temMetodoTransacional(Class<?> tipo) {
        return Arrays.stream(tipo.getMethods()).anyMatch(m -> m.isAnnotationPresent(Transacional.class));
    }
}
