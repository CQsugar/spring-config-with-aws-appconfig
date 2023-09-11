package com.example.springconfigwithawsappconfig.aws.appconfig;

import com.example.springconfigwithawsappconfig.aws.refresh.AppConfigContextRefresher;
import com.example.springconfigwithawsappconfig.aws.refresh.AppConfigRefreshHistory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppConfigProperties.class)
@ConditionalOnProperty(name = "aws.appconfig.enabled", matchIfMissing = true)
public class AwsAppConfigAutoConfiguration {


    @Bean
    public AppConfigRefreshHistory appConfigRefreshHistory() {
        return new AppConfigRefreshHistory();
    }

    @Bean
    public AppConfigContextRefresher appConfigContextRefresher(
            AppConfigProperties appConfigProperties,
            ThreadPoolTaskScheduler threadPoolTaskScheduler,
            AppConfigPropertySourceBuilder appConfigPropertySourceBuilder,
            AppConfigRefreshHistory appConfigRefreshHistory) {

        return new AppConfigContextRefresher(appConfigProperties,
                appConfigRefreshHistory, threadPoolTaskScheduler,
                appConfigPropertySourceBuilder);

    }


}
