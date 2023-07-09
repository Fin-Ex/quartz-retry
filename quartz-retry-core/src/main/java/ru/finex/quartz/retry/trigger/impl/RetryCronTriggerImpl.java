package ru.finex.quartz.retry.trigger.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Calendar;
import org.quartz.CronExpression;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.impl.triggers.AbstractTrigger;
import org.quartz.impl.triggers.CoreTrigger;
import ru.finex.quartz.retry.trigger.RetryCronTrigger;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author oracle
 */
@Slf4j
public class RetryCronTriggerImpl extends AbstractTrigger<RetryCronTrigger> implements RetryCronTrigger, CoreTrigger {

    protected static final int YEAR_TO_GIVEUP_SCHEDULING_AT = CronExpression.MAX_YEAR;

    @Setter
    @Getter
    private int retryCount = 0;

    @Getter
    @Setter
    private int retryMaxAttempts = -1;

    private boolean retrySucceed = false;

    private CronExpression cronEx = null;

    @Getter
    private Date startTime = new Date();

    @Getter
    private Date endTime = null;

    @Getter
    @Setter
    private Date nextFireTime = null;

    @Getter
    @Setter
    private Date previousFireTime = null;

    private transient TimeZone timeZone = TimeZone.getDefault();

    public void setCronExpression(String cronExpression) throws ParseException {
        TimeZone origTz = getTimeZone();
        this.cronEx = new CronExpression(cronExpression);
        this.cronEx.setTimeZone(origTz);
    }

    public String getCronExpression() {
        return cronEx == null ? null : cronEx.getCronExpression();
    }

    public void setCronExpression(CronExpression cronExpression) {
        this.cronEx = cronExpression;
        this.timeZone = cronExpression.getTimeZone();
    }

    @Override
    public void setStartTime(Date startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }

        Date eTime = getEndTime();
        if (eTime != null && eTime.before(startTime)) {
            throw new IllegalArgumentException(
                "End time cannot be before start time");
        }

        // round off millisecond...
        // Note timeZone is not needed here as parameter for
        // Calendar.getInstance(),
        // since time zone is implicit when using a Date in the setTime method.
        java.util.Calendar cl = java.util.Calendar.getInstance();
        cl.setTime(startTime);
        cl.set(java.util.Calendar.MILLISECOND, 0);

        this.startTime = cl.getTime();
    }

    @Override
    public void setEndTime(Date endTime) {
        Date sTime = getStartTime();
        if (sTime != null && endTime != null && sTime.after(endTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }

        this.endTime = endTime;
    }

    @Override
    public TimeZone getTimeZone() {
        if(cronEx != null) {
            return cronEx.getTimeZone();
        }

        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        if(cronEx != null) {
            cronEx.setTimeZone(timeZone);
        }
        this.timeZone = timeZone;
    }

    @Override
    public Date getFireTimeAfter(Date afterTime) {
        if (afterTime == null) {
            afterTime = new Date();
        }

        if (getStartTime().after(afterTime)) {
            afterTime = new Date(getStartTime().getTime() - 1000L);
        }

        if (getEndTime() != null && (afterTime.compareTo(getEndTime()) >= 0)) {
            return null;
        }

        Date pot = getTimeAfter(afterTime);
        if (getEndTime() != null && pot != null && pot.after(getEndTime())) {
            return null;
        }

        return pot;
    }

    @Override
    public Date getFinalFireTime() {
        Date resultTime;
        if (getEndTime() != null) {
            resultTime = getTimeBefore(new Date(getEndTime().getTime() + 1000L));
        } else {
            resultTime = (cronEx == null) ? null : cronEx.getFinalFireTime();
        }

        if ((resultTime != null) && (getStartTime() != null) && (resultTime.before(getStartTime()))) {
            return null;
        }

        return resultTime;
    }

    @Override
    protected boolean validateMisfireInstruction(int misfireInstruction) {
        return misfireInstruction >= MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY && misfireInstruction <= MISFIRE_INSTRUCTION_DO_NOTHING;
    }

    @Override
    public void updateAfterMisfire(org.quartz.Calendar cal) {
        int instr = getMisfireInstruction();

        if(instr == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY)
            return;

        if (instr == MISFIRE_INSTRUCTION_SMART_POLICY) {
            instr = MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
        }

        if (instr == MISFIRE_INSTRUCTION_DO_NOTHING) {
            Date newFireTime = getFireTimeAfter(new Date());
            while (newFireTime != null && cal != null
                && !cal.isTimeIncluded(newFireTime.getTime())) {
                newFireTime = getFireTimeAfter(newFireTime);
            }
            setNextFireTime(newFireTime);
        } else if (instr == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) {
            setNextFireTime(new Date());
        }
    }

    public boolean willFireOn(java.util.Calendar test) {
        return willFireOn(test, false);
    }

    public boolean willFireOn(java.util.Calendar test, boolean dayOnly) {
        test = (java.util.Calendar) test.clone();

        test.set(java.util.Calendar.MILLISECOND, 0); // don't compare millis.

        if(dayOnly) {
            test.set(java.util.Calendar.HOUR_OF_DAY, 0);
            test.set(java.util.Calendar.MINUTE, 0);
            test.set(java.util.Calendar.SECOND, 0);
        }

        Date testTime = test.getTime();

        Date fta = getFireTimeAfter(new Date(test.getTime().getTime() - 1000));

        if(fta == null)
            return false;

        java.util.Calendar p = java.util.Calendar.getInstance(test.getTimeZone());
        p.setTime(fta);

        int year = p.get(java.util.Calendar.YEAR);
        int month = p.get(java.util.Calendar.MONTH);
        int day = p.get(java.util.Calendar.DATE);

        if(dayOnly) {
            return (year == test.get(java.util.Calendar.YEAR)
                && month == test.get(java.util.Calendar.MONTH)
                && day == test.get(java.util.Calendar.DATE));
        }

        while(fta.before(testTime)) {
            fta = getFireTimeAfter(fta);
        }

        return fta.equals(testTime);
    }

    @Override
    public boolean mayFireAgain() {
        return getNextFireTime() != null && !retrySucceed && (isInfiniteRetries() || retryCount < retryMaxAttempts);
    }

    @Override
    public void triggered(Calendar calendar) {
        retryCount = retryCount + 1;
        previousFireTime = nextFireTime;
        nextFireTime = getFireTimeAfter(nextFireTime);

        while (nextFireTime != null && calendar != null
            && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            nextFireTime = getFireTimeAfter(nextFireTime);
        }
    }

    @Override
    public void updateWithNewCalendar(Calendar calendar, long misfireThreshold) {
        nextFireTime = getFireTimeAfter(previousFireTime);

        if (nextFireTime == null || calendar == null) {
            return;
        }

        Date now = new Date();
        while (nextFireTime != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {

            nextFireTime = getFireTimeAfter(nextFireTime);

            if(nextFireTime == null)
                break;

            //avoid infinite loop
            // Use gregorian only because the constant is based on Gregorian
            java.util.Calendar c = new java.util.GregorianCalendar();
            c.setTime(nextFireTime);
            if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
                nextFireTime = null;
            }

            if(nextFireTime != null && nextFireTime.before(now)) {
                long diff = now.getTime() - nextFireTime.getTime();
                if(diff >= misfireThreshold) {
                    nextFireTime = getFireTimeAfter(nextFireTime);
                }
            }
        }
    }

    @Override
    public Date computeFirstFireTime(org.quartz.Calendar calendar) {
        nextFireTime = getFireTimeAfter(new Date(getStartTime().getTime() - 1000L));

        while (nextFireTime != null && calendar != null
            && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            nextFireTime = getFireTimeAfter(nextFireTime);
        }

        return nextFireTime;
    }

    public String getExpressionSummary() {
        return cronEx == null ? null : cronEx.getExpressionSummary();
    }

    public boolean hasAdditionalProperties() {
        return false;
    }

    @Override
    public CompletedExecutionInstruction executionComplete(JobExecutionContext context, JobExecutionException result) {
        if (result == null) {
            retrySucceed = true;
        }

        return super.executionComplete(context, result);
    }

    @Override
    public ScheduleBuilder<RetryCronTrigger> getScheduleBuilder() {
        RetryCronScheduleBuilder cb = RetryCronScheduleBuilder.retriesSchedule(getCronExpression(), retryMaxAttempts)
            .inTimeZone(getTimeZone());

        int misfireInstruction = getMisfireInstruction();
        switch(misfireInstruction) {
            case MISFIRE_INSTRUCTION_SMART_POLICY:
                break;
            case MISFIRE_INSTRUCTION_DO_NOTHING:
                cb.withMisfireHandlingInstructionDoNothing();
                break;
            case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW:
                cb.withMisfireHandlingInstructionFireAndProceed();
                break;
            case MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY:
                cb.withMisfireHandlingInstructionIgnoreMisfires();
                break;
            default:
                log.warn("Unrecognized misfire policy {}. Derived builder will use the default cron " +
                    "trigger behavior (MISFIRE_INSTRUCTION_FIRE_ONCE_NOW)", misfireInstruction);
        }

        return cb;
    }

    @Override
    public Object clone() {
        RetryCronTriggerImpl copy = (RetryCronTriggerImpl) super.clone();
        if (cronEx != null) {
            copy.setCronExpression(new CronExpression(cronEx));
        }
        return copy;
    }

    protected boolean isInfiniteRetries() {
        return retryMaxAttempts == -1;
    }

    protected Date getTimeAfter(Date afterTime) {
        return (cronEx == null) ? null : cronEx.getTimeAfter(afterTime);
    }

    protected Date getTimeBefore(Date eTime) {
        return (cronEx == null) ? null : cronEx.getTimeBefore(eTime);
    }

}
