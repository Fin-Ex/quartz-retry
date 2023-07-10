package ru.finex.quartz.retry.autoconfigure;

import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import ru.finex.quartz.retry.RetryDefinitionProvider;
import ru.finex.quartz.retry.RetryableAnnotationAdvisor;
import ru.finex.quartz.retry.SpringPropertyResolver;
import ru.finex.quartz.retry.annotation.RetryableJob;
import ru.finex.quartz.retry.job.JobRetryDefinition;
import ru.finex.quartz.retry.listener.RetryableJobExecutionListener;
import ru.finex.quartz.retry.utils.PropertyResolver;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author oracle
 */
@AutoConfiguration
@AutoConfigureAfter(QuartzAutoConfiguration.class)
@ConditionalOnClass({ Scheduler.class, SchedulerFactoryBean.class })
@Import(QuartzRetryDataSourceAutoConfiguration.class)
public class QuartzRetryAutoConfiguration {

    @Autowired
    private Environment environment;

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(List<JobListener> jobListeners) {
        return schedulerFactoryBean -> schedulerFactoryBean.setGlobalJobListeners(jobListeners.toArray(JobListener[]::new));
    }

    @Bean
    @ConditionalOnMissingBean(PropertyResolver.class)
    public PropertyResolver propertyResolver(Environment environment) {
        return new SpringPropertyResolver(environment);
    }

    @Bean
    @ConditionalOnMissingBean(RetryDefinitionProvider.class)
    public RetryDefinitionProvider retryDefinitionProvider(ApplicationContext applicationContext,
                                                           PropertyResolver propertyResolver) {
        Map<Class<?>, JobRetryDefinition> definitionMap = getDefinitionMap(applicationContext, propertyResolver);
        return new RetryDefinitionProvider(definitionMap);
    }

    @Bean
    @ConditionalOnMissingBean(RetryableJobExecutionListener.class)
    public RetryableJobExecutionListener retryableJobExecutionListener(RetryDefinitionProvider retryDefinitionProvider) {
        return new RetryableJobExecutionListener(retryDefinitionProvider);
    }

    private Map<Class<?>, JobRetryDefinition> getDefinitionMap(ApplicationContext applicationContext,
                                                               PropertyResolver propertyResolver) {
        String baseApplicationPackage = getRequiredBaseApplicationPackage(applicationContext);
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(RetryableJob.class));

        return provider.findCandidateComponents(baseApplicationPackage).stream()
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

    private String getRequiredBaseApplicationPackage(ApplicationContext applicationContext) {
        return AutoConfigurationPackages.get(applicationContext.getAutowireCapableBeanFactory()).stream().findAny()
            .orElseThrow(() -> new ApplicationContextException("Unable to get application base package"));
    }

}
