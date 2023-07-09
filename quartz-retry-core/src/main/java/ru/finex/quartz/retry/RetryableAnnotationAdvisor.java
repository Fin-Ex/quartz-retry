package ru.finex.quartz.retry;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import ru.finex.quartz.retry.annotation.RetryableJob;
import ru.finex.quartz.retry.utils.PropertyResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author oracle
 */
@RequiredArgsConstructor
public class RetryableAnnotationAdvisor {

    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([\\d\\w_.-]+)(?>:'([^']*)')?}");

    private final RetryableJob retryableJob;

    private final PropertyResolver propertyResolver;

    public String getJobRetryCron() {
        return resolveAnnotationProperty(retryableJob.cron());
    }

    public int getJobRetryMaxAttempts() {
        return Integer.parseInt(resolveAnnotationProperty(retryableJob.maxAttempts()));
    }

    private String resolveAnnotationProperty(String propertyValue) {
        if (isEnvPlaceholder(propertyValue)) {
            return resolve(propertyValue);
        }

        return propertyValue;
    }

    private String resolve(String expression) {
        Matcher matcher = ENV_PATTERN.matcher(expression);

        String result = expression;
        while (matcher.find()) {
            String envName = matcher.group(1);
            String defaultValue = ObjectUtils.defaultIfNull(matcher.group(2), ""); // todo oracle: mb throw ex
            String value = propertyResolver.getProperty(envName, defaultValue);

            result = matcher.replaceFirst(value);
            matcher = ENV_PATTERN.matcher(result);
        }

        return result;
    }

    private boolean isEnvPlaceholder(String value) {
        return ENV_PATTERN.matcher(value).matches();
    }

}
