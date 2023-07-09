package ru.finex.quartz.retry.utils;

/**
 * @author oracle
 */
public interface PropertyResolver {

    String getProperty(String key, String defaultValue);

}
