package ru.finex.quartz.retry;

import org.springframework.context.annotation.Import;
import ru.finex.quartz.retry.autoconfigure.QuartzPersistenceDelegatesAutoConfiguration;
import ru.finex.quartz.retry.autoconfigure.RetryableQuartzAutoConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author oracle
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ QuartzPersistenceDelegatesAutoConfiguration.class, RetryableQuartzAutoConfiguration.class })
public @interface EnableQuartzRetries {

}
