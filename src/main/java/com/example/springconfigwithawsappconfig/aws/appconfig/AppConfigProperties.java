package com.example.springconfigwithawsappconfig.aws.appconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Configuration
@ConfigurationProperties(prefix = AppConfigProperties.PREFIX)
public class AppConfigProperties {


    /**
     * COMMAS , .
     */
    public static final String COMMAS = ",";

    /**
     * Prefix of {@link AppConfigProperties}.
     */
    public static final String PREFIX = "spring.aws";


    private List<AppConfigInfo> config = new ArrayList<>();


    /**
     * the master switch for refresh configuration, it default opened(true).
     */
    private boolean refreshEnabled = true;

    /**
     * check update delay *
     */
    private Long fixedDelay = 10 * 1000L;


    public List<AppConfigInfo> getConfig() {
        return config;
    }

    public void setConfig(List<AppConfigInfo> config) {
        this.config = config;
    }

    public boolean isRefreshEnabled() {
        return refreshEnabled;
    }

    public void setRefreshEnabled(boolean refreshEnabled) {
        this.refreshEnabled = refreshEnabled;
    }

    public Long getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(Long fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    static class AppConfigInfo {

        /**
         * application Name
         */
        private String application;
        /**
         * environment Name
         */
        private String environment;
        /**
         * file Name
         */
        private List<String> fileNames;

        /**
         * refresh support
         */
        private boolean refresh = true;


        public String getApplication() {
            return application;
        }

        public void setApplication(String application) {
            this.application = application;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public List<String> getFileNames() {
            return fileNames;
        }

        public void setFileNames(List<String> fileNames) {
            this.fileNames = fileNames;
        }

        public boolean isRefresh() {
            return refresh;
        }

        public void setRefresh(boolean refresh) {
            this.refresh = refresh;
        }
    }
}
