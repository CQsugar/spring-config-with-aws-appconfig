package com.example.springconfigwithawsappconfig.aws.repository;


import com.example.springconfigwithawsappconfig.aws.appconfig.AppConfigProperties;
import com.example.springconfigwithawsappconfig.aws.appconfig.AppConfigPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AppConfigPropertySourceRepository {

    private static final ConcurrentHashMap<String, AppConfigPropertySource> APP_CONFIG_PROPERTY_SOURCE_REPOSITORY = new ConcurrentHashMap<>();


    private AppConfigPropertySourceRepository() {

    }

    /**
     * @return all properties from application context.
     */
    public static List<AppConfigPropertySource> getAll() {
        return new ArrayList<>(APP_CONFIG_PROPERTY_SOURCE_REPOSITORY.values());
    }

    public static void updateAppConfigPropertySource(
            AppConfigPropertySource appConfigPropertySource) {
        APP_CONFIG_PROPERTY_SOURCE_REPOSITORY
                .put(getMapKey(appConfigPropertySource.getApplication(),
                        appConfigPropertySource.getEnvironment(), appConfigPropertySource.getProfile()), appConfigPropertySource);
    }

    public static AppConfigPropertySource getAppConfigPropertySource(String application, String env, String profile) {
        return APP_CONFIG_PROPERTY_SOURCE_REPOSITORY.get(getMapKey(application, env, profile));
    }

    public static String getMapKey(String application, String env, String profile) {
        return String.join(AppConfigProperties.COMMAS, application,
                env, profile);
    }

}
