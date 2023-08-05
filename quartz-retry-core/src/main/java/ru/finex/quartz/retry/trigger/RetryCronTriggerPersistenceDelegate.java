package ru.finex.quartz.retry.trigger;

import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.StdJDBCConstants;
import org.quartz.impl.jdbcjobstore.TriggerPersistenceDelegate;
import org.quartz.impl.jdbcjobstore.Util;
import org.quartz.spi.OperableTrigger;
import ru.finex.quartz.retry.trigger.impl.RetryCronScheduleBuilder;
import ru.finex.quartz.retry.trigger.impl.RetryCronTriggerImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimeZone;

/**
 * @author oracle
 */
public class RetryCronTriggerPersistenceDelegate implements TriggerPersistenceDelegate, StdJDBCConstants {

    private static final String COL_RETRIES_COUNT = "retries_count";

    private static final String COL_MAX_ATTEMPTS = "max_attempts";

    private static final String TABLE_RETRY_CRON_TRIGGERS = "retry_cron_triggers";

    private static final String DELETE_RETRY_CRON_TRIGGER = "delete from " +
        TABLE_PREFIX_SUBST + TABLE_RETRY_CRON_TRIGGERS + " where " +
        COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " and " +
        COL_TRIGGER_NAME + " = ? and " + COL_TRIGGER_GROUP + " = ?";

    private static final String INSERT_RETRY_CRON_TRIGGER = "insert into " +
        TABLE_PREFIX_SUBST + TABLE_RETRY_CRON_TRIGGERS + " (" +
        COL_SCHEDULER_NAME + ", " + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + ", " +
        COL_CRON_EXPRESSION + ", " + COL_TIME_ZONE_ID + ", " + COL_RETRIES_COUNT  + ", " + COL_MAX_ATTEMPTS + ") " +
        " values(" + SCHED_NAME_SUBST + ", ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_RETRY_CRON_TRIGGER = "select * from " +
        TABLE_PREFIX_SUBST + TABLE_RETRY_CRON_TRIGGERS + " where " +
        COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST +
        " and " + COL_TRIGGER_NAME + " = ? and " + COL_TRIGGER_GROUP + " = ?";

    private static final String UPDATE_RETRY_CRON_TRIGGER = "update " +
        TABLE_PREFIX_SUBST + TABLE_RETRY_CRON_TRIGGERS + " set " +
        COL_CRON_EXPRESSION + " = ?, " + COL_TIME_ZONE_ID + " = ?, " +
        COL_RETRIES_COUNT + " = ?, " + COL_MAX_ATTEMPTS + " = ? " +
        "where " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST +
        " and " + COL_TRIGGER_NAME + " = ? and " + COL_TRIGGER_GROUP + " = ?";

    protected String tablePrefix;

    protected String schedNameLiteral;

    @Override
    public void initialize(String theTablePrefix, String schedName) {
        this.tablePrefix = theTablePrefix;
        this.schedNameLiteral = "'" + schedName + "'";
    }

    @Override
    public String getHandledTriggerTypeDiscriminator() {
        return "R_CRON";
    }

    @Override
    public boolean canHandleTriggerType(OperableTrigger trigger) {
        return trigger instanceof RetryCronTriggerImpl && !((RetryCronTriggerImpl)trigger).hasAdditionalProperties();
    }

    @Override
    public int deleteExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(Util.rtp(DELETE_RETRY_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2, triggerKey.getGroup());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

    @Override
    public int insertExtendedTriggerProperties(Connection conn, OperableTrigger trigger,
                                               String state, JobDetail jobDetail) throws SQLException {
        RetryCronTriggerImpl retryCronTrigger = (RetryCronTriggerImpl) trigger;

        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(Util.rtp(INSERT_RETRY_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, trigger.getKey().getName());
            ps.setString(2, trigger.getKey().getGroup());
            ps.setString(3, retryCronTrigger.getCronExpression());
            ps.setString(4, retryCronTrigger.getTimeZone().getID());
            ps.setInt(5, retryCronTrigger.getRetryCount());
            ps.setInt(6, retryCronTrigger.getRetryMaxAttempts());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

    @Override
    public TriggerPropertyBundle loadExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(Util.rtp(SELECT_RETRY_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2, triggerKey.getGroup());
            rs = ps.executeQuery();

            if (rs.next()) {
                String cronExpr = rs.getString(COL_CRON_EXPRESSION);
                String timeZoneId = rs.getString(COL_TIME_ZONE_ID);
                int maxAttempts = rs.getInt(COL_MAX_ATTEMPTS);
                int retryCount = rs.getInt(COL_RETRIES_COUNT);

                RetryCronScheduleBuilder cb = RetryCronScheduleBuilder.retriesSchedule(cronExpr, maxAttempts);

                if (timeZoneId != null) {
                    cb.inTimeZone(TimeZone.getTimeZone(timeZoneId));
                }

                return new TriggerPropertyBundle(cb, new String[]{"retryCount"}, new Object[]{retryCount});
            }

            throw new IllegalStateException("No record found for selection of Trigger with key: '" +
                triggerKey + "' and statement: " + Util.rtp(SELECT_CRON_TRIGGER, tablePrefix, schedNameLiteral));
        } finally {
            Util.closeResultSet(rs);
            Util.closeStatement(ps);
        }
    }

    @Override
    public int updateExtendedTriggerProperties(Connection conn, OperableTrigger trigger,
                                               String state, JobDetail jobDetail) throws SQLException {
        RetryCronTriggerImpl retryCronTrigger = (RetryCronTriggerImpl) trigger;

        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(Util.rtp(UPDATE_RETRY_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, retryCronTrigger.getCronExpression());
            ps.setString(2, retryCronTrigger.getTimeZone().getID());
            ps.setInt(3, retryCronTrigger.getRetryCount());
            ps.setInt(4, retryCronTrigger.getRetryMaxAttempts());
            ps.setString(5, trigger.getKey().getName());
            ps.setString(6, trigger.getKey().getGroup());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

}
