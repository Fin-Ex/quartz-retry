package ru.finex.quartz.retry.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import ru.finex.quartz.retry.utils.QuartzJobUtils;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * @author oracle
 */
public abstract class AbstractJobListener implements JobListener {

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        if (jobException == null) {
            LocalDate currentDate = LocalDate.ofInstant(context.getFireTime().toInstant(), ZoneId.systemDefault());
            context.getJobDetail().getJobDataMap().put(QuartzJobUtils.LAST_JOB_SUCCESS_VARIABLE, currentDate);
            jobExecutionCompleted(context);
            return;
        }

        jobExecutionFailed(context, jobException);
    }

    public abstract void jobExecutionCompleted(JobExecutionContext context);

    public abstract void jobExecutionFailed(JobExecutionContext context, JobExecutionException jobException);

}
