package ru.finex.quartz.retry;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import ru.finex.quartz.retry.utils.PropertyResolver;

/**
 * @author oracle
 */
@RequiredArgsConstructor
public class SpringPropertyResolver implements PropertyResolver {

    private final Environment environment;

    @Override
    public String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

}
