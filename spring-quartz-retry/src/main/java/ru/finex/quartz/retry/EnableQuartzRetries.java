package ru.finex.quartz.retry;

import org.springframework.context.annotation.Import;
import ru.finex.quartz.retry.autoconfigure.QuartzPlatformDriverAutoConfiguration;
import ru.finex.quartz.retry.autoconfigure.QuartzRetryAutoConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author oracle
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ QuartzPlatformDriverAutoConfiguration.class, QuartzRetryAutoConfiguration.class })
public @interface EnableQuartzRetries {

}
