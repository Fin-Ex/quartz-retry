package ru.finex.quartz.retry;

import lombok.RequiredArgsConstructor;
import ru.finex.quartz.retry.job.JobRetryDefinition;

import java.util.Map;
import java.util.Optional;

/**
 * @author oracle
 */
@RequiredArgsConstructor
public class RetryDefinitionProvider {

    private final Map<Class<?>, JobRetryDefinition> definitionMap;

    public Optional<JobRetryDefinition> getDefinition(Class<?> jobClass) {
        return Optional.ofNullable(definitionMap.get(jobClass));
    }

}
