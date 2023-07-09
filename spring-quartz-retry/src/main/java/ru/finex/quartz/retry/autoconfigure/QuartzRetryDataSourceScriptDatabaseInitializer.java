package ru.finex.quartz.retry.autoconfigure;

import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.init.PlatformPlaceholderDatabaseDriverResolver;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.util.StringUtils;

import java.util.List;
import javax.sql.DataSource;

/**
 * @author oracle
 */
public class QuartzRetryDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer {

    public QuartzRetryDataSourceScriptDatabaseInitializer(DataSource dataSource, QuartzProperties properties,
                                                          String... schemaLocations) {
        this(dataSource, getSettings(dataSource, properties, schemaLocations));
    }

    public QuartzRetryDataSourceScriptDatabaseInitializer(DataSource dataSource, DatabaseInitializationSettings settings) {
        super(dataSource, settings);
    }

    private static DatabaseInitializationSettings getSettings(DataSource dataSource, QuartzProperties properties,
                                                              String... schemaLocations) {
        DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
        settings.setSchemaLocations(resolveSchemaLocations(dataSource, properties.getJdbc().getPlatform(), schemaLocations));
        settings.setMode(properties.getJdbc().getInitializeSchema());
        settings.setContinueOnError(true);
        return settings;
    }

    private static List<String> resolveSchemaLocations(DataSource dataSource, String jdbcPlatform,
                                                       String... schemaLocations) {
        PlatformPlaceholderDatabaseDriverResolver platformResolver = new PlatformPlaceholderDatabaseDriverResolver();
        platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.DB2, "db2_v95");
        platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.MYSQL, "mysql_innodb");
        platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.MARIADB, "mysql_innodb");
        platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.POSTGRESQL, "postgres");
        platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.SQLSERVER, "sqlServer");

        if (StringUtils.hasText(jdbcPlatform)) {
            return platformResolver.resolveAll(jdbcPlatform, schemaLocations);
        }

        return platformResolver.resolveAll(dataSource, schemaLocations);
    }

}
