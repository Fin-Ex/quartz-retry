package ru.finex.quartz.retry.autoconfigure;

import org.apache.commons.lang3.tuple.Pair;
import org.quartz.Scheduler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import ru.finex.quartz.retry.trigger.RetryCronTriggerPersistenceDelegate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author oracle
 */
@AutoConfiguration
@AutoConfigureBefore(QuartzAutoConfiguration.class)
@ConditionalOnClass({ Scheduler.class, SchedulerFactoryBean.class })
public class QuartzPlatformDriverAutoConfiguration implements BeanPostProcessor {

    private static final String QUARTZ_JOB_STORE_DRIVER_DELEGATE_INIT_PROP = "org.quartz.jobStore.driverDelegateInitString";

    private static final String QUARTZ_JOB_STORE_TRIGGER_PERSISTENCE_DELEGATE_PROP = "triggerPersistenceDelegateClasses";

    private static final List<Class<?>> CUSTOM_TRIGGER_PERSISTENCE_DELEGATES = List.of(RetryCronTriggerPersistenceDelegate.class);

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        if (bean instanceof QuartzProperties) {
            postProcessQuartzPersistenceDelegates((QuartzProperties) bean);
        }

        return bean;
    }

    protected void postProcessQuartzPersistenceDelegates(QuartzProperties config) {
        config.getProperties().compute(QUARTZ_JOB_STORE_DRIVER_DELEGATE_INIT_PROP, (key, driverDelegateInitString) -> {
            String persistenceDelegates = CUSTOM_TRIGGER_PERSISTENCE_DELEGATES.stream().map(Class::getCanonicalName)
                .collect(Collectors.joining(","));
            String triggerPersistenceDelegateClassesProperty = QUARTZ_JOB_STORE_TRIGGER_PERSISTENCE_DELEGATE_PROP +
                "=" + persistenceDelegates;

            if (driverDelegateInitString == null) {
                return triggerPersistenceDelegateClassesProperty;
            }

            if (!driverDelegateInitString.contains(QUARTZ_JOB_STORE_TRIGGER_PERSISTENCE_DELEGATE_PROP)) {
                return driverDelegateInitString + "|" + triggerPersistenceDelegateClassesProperty;
            }

            return Arrays.stream(driverDelegateInitString.split("\\|"))
                .map(property -> {
                    String[] propertyKeyValue = property.split("=");
                    return Pair.of(propertyKeyValue[0], propertyKeyValue[1]);
                })
                .map(propertyPair -> {
                    if (QUARTZ_JOB_STORE_TRIGGER_PERSISTENCE_DELEGATE_PROP.equals(propertyPair.getKey())) {
                        return triggerPersistenceDelegateClassesProperty + "," + propertyPair.getValue();
                    }

                    return propertyPair.getKey() + "=" + propertyPair.getValue();
                })
                .collect(Collectors.joining("|"));
        });
    }

}
