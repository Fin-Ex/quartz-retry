package ru.finex.quartz.retry.trigger.impl;

import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.spi.MutableTrigger;
import ru.finex.quartz.retry.trigger.RetryCronTrigger;

import java.text.ParseException;
import java.util.TimeZone;

/**
 * @author oracle
 */
public class RetryCronScheduleBuilder extends ScheduleBuilder<RetryCronTrigger> {

    private final int maxAttempts;

    private final CronExpression cronExpression;

    private int misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

    protected RetryCronScheduleBuilder(CronExpression cronExpression, int maxAttempts) {
        if (cronExpression == null) {
            throw new IllegalArgumentException("cronExpression must not be null!");
        }

        this.maxAttempts = maxAttempts;
        this.cronExpression = cronExpression;
    }

    @Override
    protected MutableTrigger build() {
        RetryCronTriggerImpl trigger = new RetryCronTriggerImpl();

        trigger.setCronExpression(cronExpression);
        trigger.setTimeZone(cronExpression.getTimeZone());
        trigger.setMisfireInstruction(misfireInstruction);
        trigger.setRetryMaxAttempts(maxAttempts);

        return trigger;
    }

    /**
     * Create a {@link RetryCronScheduleBuilder} with the given {@literal cronExpression} string and
     * {@literal maxAttempts} for retries configuration.
     *
     * @param cronExpression the cron expression string to base the schedule on.
     * @param maxAttempts max retries attempts configuration.
     * @return a new instance of {@link RetryCronScheduleBuilder}.
     * @throws RuntimeException wrapping a {@link ParseException} if provided {@literal cronExpression} invalid.
     * */
    public static RetryCronScheduleBuilder retriesSchedule(String cronExpression, int maxAttempts) {
        try {
            return retriesSchedule(new CronExpression(cronExpression), maxAttempts);
        } catch (ParseException e) {
            // all methods of construction ensure the expression is valid by
            // this point...
            throw new RuntimeException("CronExpression '" + cronExpression + "' is invalid.", e);
        }
    }

    /**
     * Create a {@link RetryCronScheduleBuilder} with the given parsed {@literal cronExpression} and
     * {@literal maxAttempts} for retries configuration.
     *
     * @param cronExpression the parsed cron expression to base the schedule on.
     * @param maxAttempts max retries attempts configuration.
     * @return a new instance of {@link RetryCronScheduleBuilder}.
     * */
    public static RetryCronScheduleBuilder retriesSchedule(CronExpression cronExpression, int maxAttempts) {
        return new RetryCronScheduleBuilder(cronExpression, maxAttempts);
    }

    /**
     * The {@link TimeZone} in which to base the schedule.
     *
     * @param timezone the time-zone for the schedule.
     * @return the updated CronScheduleBuilder
     * @see CronExpression#getTimeZone()
     */
    public RetryCronScheduleBuilder inTimeZone(TimeZone timezone) {
        cronExpression.setTimeZone(timezone);
        return this;
    }

    /**
     * If the Trigger misfires, use the
     * {@link Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY} instruction.
     *
     * @return the updated CronScheduleBuilder
     * @see Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
     */
    public RetryCronScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires() {
        misfireInstruction = Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
        return this;
    }

    /**
     * If the Trigger misfires, use the
     * {@link CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING} instruction.
     *
     * @return the updated CronScheduleBuilder
     * @see CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
     */
    public RetryCronScheduleBuilder withMisfireHandlingInstructionDoNothing() {
        misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
        return this;
    }

    /**
     * If the Trigger misfires, use the
     * {@link CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} instruction.
     *
     * @return the updated CronScheduleBuilder
     * @see CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
     */
    public RetryCronScheduleBuilder withMisfireHandlingInstructionFireAndProceed() {
        misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
        return this;
    }

}
