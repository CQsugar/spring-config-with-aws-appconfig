package com.example.springconfigwithawsappconfig.aws.appconfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;


@EnableConfigurationProperties(AppConfigProperties.class)
@ConditionalOnProperty(name = "aws.appconfig.enabled", matchIfMissing = true)
public class AwsAppConfigBootStrapConfiguration {

    @Bean
    public AppConfigDataClient appConfigDataClient() {
        return AppConfigDataClient.create();
    }

    @Bean
    public AppConfigPropertySourceBuilder appConfigPropertySourceBuilder(AppConfigDataClient appConfigDataClient) {
        return new AppConfigPropertySourceBuilder(appConfigDataClient);
    }

    @Bean
    public AppConfigPropertySourceLocator appConfigPropertySourceLocator(AppConfigProperties appConfigProperties,
                                                                         AppConfigPropertySourceBuilder appConfigPropertySourceBuilder) {
        return new AppConfigPropertySourceLocator(appConfigProperties, appConfigPropertySourceBuilder);

    }


}
