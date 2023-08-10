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
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author oracle
 */
@Slf4j
public class RetryCronTriggerImpl extends AbstractTrigger<RetryCronTrigger> implements RetryCronTrigger, CoreTrigger {

    protected static final int YEAR_TO_GIVEUP_SCHEDULING_AT = CronExpression.MAX_YEAR;

    @Setter
    @Getter
    private int retryCount;

    @Getter
    @Setter
    private int retryMaxAttempts = -1;

    private boolean retrySucceed;

    private CronExpression cronEx;

    @Getter
    private Date startTime = new Date();

    @Getter
    private Date endTime;

    @Getter
    @Setter
    private Date nextFireTime;

    @Getter
    @Setter
    private Date previousFireTime;

    private transient TimeZone timeZone = TimeZone.getDefault();

    @Override
    public String getCronExpression() {
        if (cronEx != null) {
            return cronEx.getCronExpression();
        } else {
            return null;
        }
    }

    @Override
    public void setCronExpression(String cronExpression) throws ParseException {
        TimeZone origTz = getTimeZone();
        this.cronEx = new CronExpression(cronExpression);
        this.cronEx.setTimeZone(origTz);
    }

    @Override
    public void setCronExpression(CronExpression cronExpression) {
        this.cronEx = cronExpression;
        this.timeZone = cronExpression.getTimeZone();
    }

    @Override
    public TimeZone getTimeZone() {
        if (cronEx != null) {
            return cronEx.getTimeZone();
        }

        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        return timeZone;
    }

    @Override
    public void setTimeZone(TimeZone timeZone) {
        if (cronEx != null) {
            cronEx.setTimeZone(timeZone);
        }
        this.timeZone = timeZone;
    }

    @Override
    public String getExpressionSummary() {
        return cronEx == null ? null : cronEx.getExpressionSummary();
    }

    @Override
    public ScheduleBuilder<RetryCronTrigger> getScheduleBuilder() {
        RetryCronScheduleBuilder cb = RetryCronScheduleBuilder.retriesSchedule(getCronExpression(), retryMaxAttempts)
            .inTimeZone(getTimeZone());

        int misfireInstruction = getMisfireInstruction();
        if (misfireInstruction!=MISFIRE_INSTRUCTION_SMART_POLICY){
            MisfireInstruction misfireHandler = MisfireInstruction.createMisfireHandler(misfireInstruction);
            misfireHandler.getMisfireBehavior(cb);
        }
        return cb;
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
    public Date getFireTimeAfter(Date afterTimeSrc) {
        Date afterTime = afterTimeSrc != null ? afterTimeSrc : new Date();

        if (getStartTime().after(afterTime)) {
            afterTime = new Date(getStartTime().getTime() - 1000L);
        }

        if (getEndTime() != null && afterTime.compareTo(getEndTime()) >= 0) {
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

        if (resultTime != null && getStartTime() != null && resultTime.before(getStartTime())) {
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
        Map<Integer, MisfireHandler> misfireHandlers = new HashMap<>();
        misfireHandlers.put(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY, new MisfireInsIngnoreMisfirePolicy());
        misfireHandlers.put(MISFIRE_INSTRUCTION_SMART_POLICY, new MisfireSmartPolicy());
        misfireHandlers.put(MISFIRE_INSTRUCTION_DO_NOTHING, new MisfireInstDoNothing());
        misfireHandlers.put(MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new MisfireInsFireOnceNow());

        int instr = getMisfireInstruction();
        MisfireHandler handler = misfireHandlers.get(instr);

        if (handler != null) {
            handler.handleMisfire(this, cal);
        }
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

        while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            nextFireTime = getFireTimeAfter(nextFireTime);
        }
    }

    @Override
    public void updateWithNewCalendar(Calendar calendar, long misfireThreshold) {
        recalculateNextFireTime(calendar, misfireThreshold);
    }

    private void recalculateNextFireTime(Calendar calendar, long misfireThreshold) {
        nextFireTime = calculateNextFireTime(nextFireTime);

        while (nextFireTime != null && !isTimeIncludedInCalendar(calendar, nextFireTime)) {
            nextFireTime = calculateNextFireTime(nextFireTime);

            if (nextFireTime == null || isYearGreaterThanThreshold(nextFireTime)) {
                break;
            }

            if (isNextFireTimeBeforeNow(nextFireTime)) {
                handleMisfireThreshold(nextFireTime, misfireThreshold);
            }
        }
    }

    private Date calculateNextFireTime(Date fireTime) {
        return (fireTime != null) ? getFireTimeAfter(fireTime) : null;
    }

    private boolean isTimeIncludedInCalendar(Calendar calendar, Date time) {
        return calendar != null && calendar.isTimeIncluded(time.getTime());
    }

    private boolean isYearGreaterThanThreshold(Date date) {
        java.util.Calendar calendar = new java.util.GregorianCalendar();
        calendar.setTime(date);
        return calendar.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT;
    }

    private boolean isNextFireTimeBeforeNow(Date nextFireTime) {
        Date now = new Date();
        return nextFireTime != null && nextFireTime.before(now);
    }

    private void handleMisfireThreshold(Date nextFireTime, long misfireThreshold) {
        Date now = new Date();
        long diff = now.getTime() - nextFireTime.getTime();
        if (diff >= misfireThreshold) {
            nextFireTime = calculateNextFireTime(nextFireTime);
        }
    }


    @Override
    public Date computeFirstFireTime(org.quartz.Calendar calendar) {
        nextFireTime = getFireTimeAfter(new Date(getStartTime().getTime() - 1000L));

        while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            nextFireTime = getFireTimeAfter(nextFireTime);
        }

        return nextFireTime;
    }

    @Override
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
        return cronEx == null ? null : cronEx.getTimeAfter(afterTime);
    }

    protected Date getTimeBefore(Date eTime) {
        return cronEx == null ? null : cronEx.getTimeBefore(eTime);
    }

}
