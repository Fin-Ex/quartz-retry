package ru.finex.quartz.retry.autoconfigure;

import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import ru.finex.quartz.retry.RetryDefinitionProvider;
import ru.finex.quartz.retry.RetryableAnnotationAdvisor;
import ru.finex.quartz.retry.SpringPropertyResolver;
import ru.finex.quartz.retry.annotation.RetryableJob;
import ru.finex.quartz.retry.job.JobRetryDefinition;
import ru.finex.quartz.retry.utils.PropertyResolver;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author oracle
 */
@AutoConfiguration
@AutoConfigureBefore(QuartzAutoConfiguration.class)
@ConditionalOnClass({ Scheduler.class, SchedulerFactoryBean.class })
public class RetryableQuartzAutoConfiguration {

    @Autowired
    private Environment environment;

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(List<JobListener> jobListeners) {
        return schedulerFactoryBean -> schedulerFactoryBean.setGlobalJobListeners(jobListeners.toArray(JobListener[]::new));
    }

    @Bean
    public PropertyResolver propertyResolver(Environment environment) {
        return new SpringPropertyResolver(environment);
    }

    @Bean
    public RetryDefinitionProvider retryDefinitionProvider(PropertyResolver propertyResolver) {
        Map<Class<?>, JobRetryDefinition> definitionMap = getDefinitionMap(propertyResolver);
        return new RetryDefinitionProvider(definitionMap);
    }

    private Map<Class<?>, JobRetryDefinition> getDefinitionMap(PropertyResolver propertyResolver) {
        String basePackage = ""; // fixme oracle: autodetect
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(RetryableJob.class));

        return provider.findCandidateComponents(basePackage).stream()
            .map(BeanDefinition::getBeanClassName)
            .map(beanClassName -> {
                try {
                    return Class.forName(beanClassName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            })
            .map(jobClass -> getJobRetryDefinition(jobClass, propertyResolver))
            .collect(Collectors.toMap(JobRetryDefinition::getJobClass, Function.identity()));
    }

    private JobRetryDefinition getJobRetryDefinition(Class<?> jobClass, PropertyResolver propertyResolver) {
        RetryableJob annotation = jobClass.getAnnotation(RetryableJob.class);
        RetryableAnnotationAdvisor retryableAnnotationAdvisor = new RetryableAnnotationAdvisor(annotation, propertyResolver);

        return JobRetryDefinition.builder().jobClass(jobClass)
            .retryCron(retryableAnnotationAdvisor.getJobRetryCron())
            .maxAttempts(retryableAnnotationAdvisor.getJobRetryMaxAttempts())
            .build();
    }

}
