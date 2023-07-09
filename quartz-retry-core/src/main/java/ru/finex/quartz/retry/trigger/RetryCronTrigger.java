package ru.finex.quartz.retry.trigger;

import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author oracle
 */
public interface RetryCronTrigger extends Trigger {

    /**
     * <p>
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>{@link CronTrigger}</code> wants to be fired now
     * by <code>Scheduler</code>.
     * </p>
     */
    int MISFIRE_INSTRUCTION_FIRE_ONCE_NOW = 1;

    /**
     * <p>
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>{@link CronTrigger}</code> wants to have it's
     * next-fire-time updated to the next time in the schedule after the
     * current time (taking into account any associated <code>{@link Calendar}</code>,
     * but it does not want to be fired now.
     * </p>
     */
    int MISFIRE_INSTRUCTION_DO_NOTHING = 2;

    /**
     * Get the cron expression string for this {@link RetryCronTrigger}.
     * @return cron expression string.
     * */
    String getCronExpression();

    /**
     * Parse and set the {@literal cronExpression} to the given one. The TimeZone on the passed-in
     * CronExpression over-rides any that was already set on the Trigger.
     * @param cronExpression an expression to be parsed and set.
     */
    void setCronExpression(String cronExpression) throws ParseException;

    /**
     * Set the {@link CronExpression} to the given one. The TimeZone on the passed-in
     * {@literal cronExpression} over-rides any that was already set on the Trigger.
     * @param cronExpression an expression to be set.
     */
    void setCronExpression(CronExpression cronExpression);

    /**
     * Get the time zone for which the {@literal cronExpression} of this {@link RetryCronTrigger} will be resolved.
     * @return a {@link TimeZone} for this {@link RetryCronTrigger}.
     */
    TimeZone getTimeZone();

    /**
     * Set the time zone for this {@link RetryCronTrigger} and its cron expression.
     * @param timeZone a time zone to be set.
     * */
    void setTimeZone(TimeZone timeZone);

    /**
     * Get summary for cron expression of this {@link RetryCronTrigger}.
     * @return a summarized human-readable cron expression string.
     * */
    String getExpressionSummary();

    /**
     * Get a {@link TriggerBuilder} that is configured to produce a {@link RetryCronTrigger} identical to this one.
     * @return a {@link TriggerBuilder}.
     * */
    TriggerBuilder<RetryCronTrigger> getTriggerBuilder();

}
