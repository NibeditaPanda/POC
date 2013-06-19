package com.tesco.adapters.core;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

public class Configuration {
    private static PropertiesConfiguration instance;

    public static PropertiesConfiguration get() throws ConfigurationException {
        if (instance == null) {
            String app_env = System.getProperty("environment");
            if (StringUtils.isBlank(app_env)) {
                LoggerFactory.getLogger(Configuration.class)
                        .warn("No environment specified. Setting it to test for test runs. " +
                                "You should have this property specified for PRODUCTION deployment. Setting to test by default...");
                app_env = "test";
            }
            instance = new PropertiesConfiguration(app_env + ".properties");
        }
        return instance;
    }
}
