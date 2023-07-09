package ru.finex.quartz.retry.utils;

/**
 * @author oracle
 */
public interface PropertyResolver {

    /**
     * Return the property value associated with the given key, or {@literal defaultValue} if the key cannot be resolved.
     * @param key the property name to resolve.
     * @param defaultValue the default value to return if no value is found.
     * @return property value if found or else {@literal defaultValue}.
     * */
    String getProperty(String key, String defaultValue);

}
