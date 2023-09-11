package com.example.springconfigwithawsappconfig.aws.appconfig;

import lombok.EqualsAndHashCode;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = false)
public class AppConfigPropertySource extends MapPropertySource {

    private final String application;

    private final String environment;

    private final String profile;

    private String nextToken;

    private final Date timestamp;
    /**
     * Whether to support dynamic refresh for this Property Source.
     */
    private final boolean isRefreshable;

    AppConfigPropertySource(String application, String environment, String profile, String nexToken,
                            Map<String, Object> source, Date timestamp, boolean isRefreshable) {
        super(String.join(AppConfigProperties.COMMAS, application, environment, profile), source);
        this.application = application;
        this.environment = environment;
        this.profile = profile;
        this.nextToken = nexToken;
        this.timestamp = timestamp;
        this.isRefreshable = isRefreshable;
    }

    public AppConfigPropertySource(List<PropertySource<?>> propertySources, String application,
                                   String environment, String profile, String nextToken, Date timestamp, boolean isRefreshable) {
        this(application, environment, profile, nextToken,
                getSourceMap(application, environment, profile, propertySources),
                timestamp, isRefreshable);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getSourceMap(String applicationId,
                                                    String environment,
                                                    String profileId,
                                                    List<PropertySource<?>> propertySources) {
        if (CollectionUtils.isEmpty(propertySources)) {
            return Collections.emptyMap();
        }
        // If only one, return the internal element, otherwise wrap it.
        if (propertySources.size() == 1) {
            PropertySource<?> propertySource = propertySources.get(0);
            if (propertySource != null && propertySource.getSource() instanceof Map) {
                return (Map<String, Object>) propertySource.getSource();
            }
        }

        Map<String, Object> sourceMap = new LinkedHashMap<>();
        List<PropertySource<?>> otherTypePropertySources = new ArrayList<>();
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource == null) {
                continue;
            }
            if (propertySource instanceof MapPropertySource mapPropertySource) {
                // If the configuration file uses "---" to separate property name,
                // propertySources will be multiple documents, and every document is a
                // map.
                // see org.springframework.boot.env.YamlPropertySourceLoader#load
                Map<String, Object> source = mapPropertySource.getSource();
                sourceMap.putAll(source);
            } else {
                otherTypePropertySources.add(propertySource);
            }
        }

        // Other property sources which is not instanceof MapPropertySource will be put as
        // it is,
        // and the internal elements cannot be directly retrieved,
        // so the user needs to implement the retrieval logic by himself
        if (!otherTypePropertySources.isEmpty()) {
            sourceMap.put(String.join(AppConfigProperties.COMMAS, applicationId, environment, profileId),
                    otherTypePropertySources);
        }
        return sourceMap;
    }

    public String getApplication() {
        return application;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getProfile() {
        return profile;
    }

    public boolean isRefreshable() {
        return isRefreshable;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getNextToken() {
        return nextToken;
    }

    public void setNextToken(String nextToken) {
        this.nextToken = nextToken;
    }
}
