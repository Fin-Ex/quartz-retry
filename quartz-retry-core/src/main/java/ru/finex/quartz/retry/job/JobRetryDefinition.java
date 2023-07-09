package ru.finex.quartz.retry.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author oracle
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRetryDefinition {

    private Class<?> jobClass;

    private String retryCron;

    private Integer maxAttempts;

}
