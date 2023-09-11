package com.example.springconfigwithawsappconfig.aws.appconfig;

import com.example.springconfigwithawsappconfig.aws.repository.AppConfigPropertySourceRepository;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;


@Order(0)
public class AppConfigPropertySourceLocator implements PropertySourceLocator {


    private static final String APP_CONFIG_PROPERTY_SOURCE_NAME = "AWS_APP_CONFIG";


    private final AppConfigPropertySourceBuilder appConfigPropertySourceBuilder;

    private final AppConfigProperties appConfigProperties;


    public AppConfigPropertySourceLocator(AppConfigProperties appConfigProperties,
                                          AppConfigPropertySourceBuilder appConfigPropertySourceBuilder) {
        this.appConfigProperties = appConfigProperties;
        this.appConfigPropertySourceBuilder = appConfigPropertySourceBuilder;
    }

    @Override
    public PropertySource<?> locate(Environment env) {

        CompositePropertySource composite = new CompositePropertySource(APP_CONFIG_PROPERTY_SOURCE_NAME);

        List<AppConfigProperties.AppConfigInfo> appConfigInfos = appConfigProperties.getConfig();
        for (AppConfigProperties.AppConfigInfo appConfigInfo : appConfigInfos) {
            String environment = appConfigInfo.getEnvironment();
            String application = appConfigInfo.getApplication();

            for (String fileName : appConfigInfo.getFileNames()) {
                loadAppConfigDataIfPresent(composite, application, environment,
                        fileName, appConfigInfo.isRefresh());
            }
        }

        return composite;
    }

    private void loadAppConfigDataIfPresent(final CompositePropertySource composite,
                                            final String application, final String env,
                                            final String profile, boolean isRefreshable) {
        if (!StringUtils.hasLength(application)) {
            return;
        }
        if (!StringUtils.hasLength(env)) {
            return;
        }
        if (!StringUtils.hasLength(profile)) {
            return;
        }

        AppConfigPropertySource propertySource = this.loadAppConfigPropertySource(application, env, profile, isRefreshable);
        this.addFirstPropertySource(composite, propertySource);
    }


    private AppConfigPropertySource loadAppConfigPropertySource(final String application, final String environment,
                                                                final String profile, boolean isRefreshable) {
        AppConfigPropertySource appConfigPropertySource = AppConfigPropertySourceRepository.getAppConfigPropertySource(application, environment, profile);
        if (Objects.isNull(appConfigPropertySource)) {
            appConfigPropertySource = appConfigPropertySourceBuilder.build(application, environment, profile,
                    isRefreshable);
        }

        return appConfigPropertySource;
    }

    /**
     * Add the appconfig configuration to the first place and maybe ignore the empty
     * configuration.
     */
    private void addFirstPropertySource(final CompositePropertySource composite,
                                        final AppConfigPropertySource propertySource) {
        if (Objects.isNull(propertySource) || Objects.isNull(composite)) {
            return;
        }
        if (propertySource.getSource().isEmpty()) {
            return;
        }
        composite.addFirstPropertySource(propertySource);
    }

}
