package ru.finex.quartz.retry.utils;

import lombok.experimental.UtilityClass;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.utils.Key;

/**
 * @author oracle
 */
@UtilityClass
public class QuartzJobUtils {

    private static final String RETRY_TRIGGER_GROUP = "$$RetryFork$$";

    public final String LAST_JOB_SUCCESS_VARIABLE = "{job:lastSuccessAt}";

    /**
     * Build a quartz's job key.
     * @param jobClass job type.
     * @return a {@link JobKey}.
     * */
    public JobKey getJobKey(Class<?> jobClass) {
        return JobKey.jobKey(jobClass.getCanonicalName(), jobClass.getClassLoader().getName());
    }

    /**
     * Build a quartz's retry trigger key.
     * @return a {@link TriggerKey}.
     * */
    public TriggerKey getRetryTriggerKey() {
        return new TriggerKey(Key.createUniqueName(RETRY_TRIGGER_GROUP), RETRY_TRIGGER_GROUP);
    }

}
