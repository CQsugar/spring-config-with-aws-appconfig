package com.example.springconfigwithawsappconfig.aws.refresh;



import com.example.springconfigwithawsappconfig.aws.appconfig.AppConfigProperties;
import com.example.springconfigwithawsappconfig.aws.appconfig.AppConfigPropertySource;
import com.example.springconfigwithawsappconfig.aws.appconfig.AppConfigPropertySourceBuilder;
import com.example.springconfigwithawsappconfig.aws.repository.AppConfigPropertySourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppConfigContextRefresher implements ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware {

    private static final Logger log = LoggerFactory
            .getLogger(AppConfigContextRefresher.class);

    private final AppConfigProperties appConfigProperties;

    private final AppConfigRefreshHistory appConfigRefreshHistory;

    private final AppConfigPropertySourceBuilder appConfigPropertySourceBuilder;

    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private ApplicationContext applicationContext;

    private final AtomicBoolean ready = new AtomicBoolean(false);

    private final Set<String> keys = new HashSet<>();

    public AppConfigContextRefresher(AppConfigProperties appConfigProperties,
                                     AppConfigRefreshHistory appConfigRefreshHistory,
                                     ThreadPoolTaskScheduler threadPoolTaskScheduler,
                                     AppConfigPropertySourceBuilder appConfigPropertySourceBuilder) {
        this.appConfigProperties = appConfigProperties;
        this.appConfigRefreshHistory = appConfigRefreshHistory;
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.appConfigPropertySourceBuilder = appConfigPropertySourceBuilder;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // many Spring context
        if (this.ready.compareAndSet(false, true)) {
            this.registerAppConfigListenersForApplications();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * register AppConfig Listeners.
     */
    private void registerAppConfigListenersForApplications() {
        if (appConfigProperties.isRefreshEnabled()) {
            for (AppConfigPropertySource propertySource : AppConfigPropertySourceRepository
                    .getAll()) {
                boolean refreshable = propertySource.isRefreshable();
                if (!refreshable) {
                    continue;
                }
                String application = propertySource.getApplication();
                String environment = propertySource.getEnvironment();
                String profile = propertySource.getProfile();
                registerAppConfigListener(application, environment, profile);
                log.info("listening config: application={}, environment={}, profile={}", application,
                        environment, profile);
            }
        }
    }

    private void registerAppConfigListener(final String application, final String environment,
                                           final String profile) {
        String key = AppConfigPropertySourceRepository.getMapKey(application, environment, profile);
        if (keys.contains(key)) {
            return;
        }
        threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
            String changedContent = appConfigPropertySourceBuilder.detectChanges(application, environment, profile);

            if (StringUtils.hasLength(changedContent)) {
                appConfigRefreshHistory.addRefreshRecord(application, environment, profile,
                        changedContent);

                log.info("config changed publish event, application:{}, environment:{}, profile:{}", application, environment, profile);
                applicationContext.publishEvent(
                        new RefreshEvent(this, null, "Refresh Appconfig config"));
            }
        }, Duration.ofMillis(appConfigProperties.getFixedDelay()));
        keys.add(key);

    }

}
