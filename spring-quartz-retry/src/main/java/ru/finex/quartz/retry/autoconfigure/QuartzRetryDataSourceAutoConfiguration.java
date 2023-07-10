package ru.finex.quartz.retry.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.autoconfigure.sql.init.OnDatabaseInitializationCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

/**
 * @author oracle
 */
@AutoConfiguration
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
    LiquibaseAutoConfiguration.class, FlywayAutoConfiguration.class })
@Conditional(QuartzRetryDataSourceAutoConfiguration.OnQuartzDatasourceInitializationCondition.class)
@ConditionalOnProperty(prefix = "spring.quartz", name = "job-store-type", havingValue = "jdbc")
@Import({ QuartzRetryDataSourceAutoConfiguration.DataSourceJanitorAutoConfiguration.class,
    QuartzRetryDataSourceAutoConfiguration.DataSourceInitializationAutoConfiguration.class })
public class QuartzRetryDataSourceAutoConfiguration {

    private static final String[] UP_DDL_CLASSPATH_LOCATIONS = {"classpath:ddl/up_tables_qrtz_retry_@@platform@@.sql"};

    private static final String[] DOWN_DDL_CLASSPATH_LOCATION = {"classpath:ddl/down_tables_qrtz_retry_@@platform@@.sql"};

    private static DataSource getDataSource(DataSource dataSource, ObjectProvider<DataSource> quartzDataSource) {
        DataSource dataSourceIfAvailable = quartzDataSource.getIfAvailable();
        return (dataSourceIfAvailable != null) ? dataSourceIfAvailable : dataSource;
    }

    @AutoConfiguration
    @AutoConfigureBefore(QuartzAutoConfiguration.class)
    protected static class DataSourceJanitorAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "quartzRetryDataSourceDownScriptDatabaseInitializer")
        public QuartzRetryDataSourceScriptDatabaseInitializer quartzRetryDataSourceDownScriptDatabaseInitializer(
            DataSource dataSource, @QuartzDataSource ObjectProvider<DataSource> quartzDataSource, QuartzProperties properties) {

            DataSource dataSourceToUse = getDataSource(dataSource, quartzDataSource);
            return new QuartzRetryDataSourceScriptDatabaseInitializer(dataSourceToUse, properties, DOWN_DDL_CLASSPATH_LOCATION);
        }

    }

    @AutoConfiguration
    @AutoConfigureAfter(QuartzAutoConfiguration.class)
    protected static class DataSourceInitializationAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "quartzRetryDataSourceUpScriptDatabaseInitializer")
        public QuartzRetryDataSourceScriptDatabaseInitializer quartzRetryDataSourceUpScriptDatabaseInitializer(
            DataSource dataSource, @QuartzDataSource ObjectProvider<DataSource> quartzDataSource, QuartzProperties properties) {

            DataSource dataSourceToUse = getDataSource(dataSource, quartzDataSource);
            return new QuartzRetryDataSourceScriptDatabaseInitializer(dataSourceToUse, properties, UP_DDL_CLASSPATH_LOCATIONS);
        }

    }

    static class OnQuartzDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

        OnQuartzDatasourceInitializationCondition() {
            super("Quartz", "spring.quartz.jdbc.initialize-schema");
        }

    }

}
