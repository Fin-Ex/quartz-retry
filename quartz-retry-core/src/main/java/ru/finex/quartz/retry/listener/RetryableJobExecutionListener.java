package ru.finex.quartz.retry.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import ru.finex.quartz.retry.RetryDefinitionProvider;
import ru.finex.quartz.retry.annotation.RetryableJob;
import ru.finex.quartz.retry.job.JobRetryDefinition;
import ru.finex.quartz.retry.trigger.RetryCronTrigger;
import ru.finex.quartz.retry.trigger.impl.RetryCronScheduleBuilder;
import ru.finex.quartz.retry.utils.QuartzJobUtils;

import java.util.Optional;

/**
 * @author oracle
 */
@Slf4j
@RequiredArgsConstructor
public class RetryableJobExecutionListener extends AbstractJobListener {

    private final RetryDefinitionProvider retryDefinitionProvider;

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        // no op
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        // no op
    }

    @Override
    public void jobExecutionCompleted(JobExecutionContext context) {
        // no op
    }

    @Override
    public void jobExecutionFailed(JobExecutionContext context, JobExecutionException jobException) {
        Optional<JobRetryDefinition> definition = retryDefinitionProvider.getDefinition(context.getJobDetail().getJobClass());

        // we shouldn't retry non-retryable jobs
        if (!isRetryableJob(context.getJobDetail().getJobClass()) || definition.isEmpty()) {
            return;
        }

        // we shouldn't handle events based on failure of another retry trigger
        if (isRetryTrigger(context.getTrigger())) {
            return;
        }

        scheduleJobRetry(context, definition.get(), jobException);
    }

    protected void scheduleJobRetry(JobExecutionContext executionCtx, JobRetryDefinition retryDefinition,
                                    JobExecutionException jobException) {
        Trigger retryTrigger = TriggerBuilder.newTrigger()
            .forJob(executionCtx.getJobDetail())
            .withPriority(executionCtx.getTrigger().getPriority())
            .withSchedule(RetryCronScheduleBuilder.retriesSchedule(retryDefinition.getRetryCron(), retryDefinition.getMaxAttempts()))
            .withIdentity(QuartzJobUtils.getRetryTriggerKey())
            .build();

        log.warn("Job [class: {}, jobKey: {}] failed and will run again according to cron schedule: [{}]",
            executionCtx.getJobDetail().getJobClass(), executionCtx.getJobDetail().getKey(), retryDefinition.getRetryCron(), jobException);

        try {
            executionCtx.getScheduler().scheduleJob(retryTrigger);
        } catch (SchedulerException ex) {
            log.error("Unable to schedule retry-job [class: {}, jobName: {}]",
                executionCtx.getJobDetail().getJobClass(), executionCtx.getJobDetail().getKey(), ex);
        }
    }

    private boolean isRetryableJob(Class<?> jobClass) {
        return jobClass.isAnnotationPresent(RetryableJob.class);
    }

    private boolean isRetryTrigger(Trigger trigger) {
        return trigger instanceof RetryCronTrigger;
    }

}
