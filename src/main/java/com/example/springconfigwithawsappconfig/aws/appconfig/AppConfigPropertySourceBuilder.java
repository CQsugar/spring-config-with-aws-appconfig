package com.example.springconfigwithawsappconfig.aws.appconfig;

import com.example.springconfigwithawsappconfig.aws.repository.AppConfigPropertySourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;
import software.amazon.awssdk.utils.Pair;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author songchunqi
 * @date 2022/8/24 16:56
 */
@Slf4j
public class AppConfigPropertySourceBuilder {

    private final AppConfigDataClient appConfigDataClient;

    public AppConfigPropertySourceBuilder(AppConfigDataClient appConfigDataClient) {
        this.appConfigDataClient = appConfigDataClient;
    }


    AppConfigPropertySource build(String application, String environment, String profile,
                                  boolean isRefreshable) {
        Pair<String, List<PropertySource<?>>> pair = loadAppConfigData(application, environment,
                profile);
        if (Objects.isNull(pair)) {
            return null;
        }
        List<PropertySource<?>> propertySources = pair.right();
        String nextToken = pair.left();
        AppConfigPropertySource appConfigPropertySource = new AppConfigPropertySource(propertySources, application,
                environment, profile, nextToken, new Date(), isRefreshable);
        AppConfigPropertySourceRepository.updateAppConfigPropertySource(appConfigPropertySource);

        return appConfigPropertySource;
    }

    private Pair<String, List<PropertySource<?>>> loadAppConfigData(String application, String environment, String profile) {
        byte[] data;
        try {
            StartConfigurationSessionResponse startConfigurationSessionResponse = appConfigDataClient.startConfigurationSession(StartConfigurationSessionRequest.builder()
                    .applicationIdentifier(application)
                    .environmentIdentifier(environment)
                    .configurationProfileIdentifier(profile).build());

            GetLatestConfigurationResponse configurationResponse = appConfigDataClient.getLatestConfiguration(GetLatestConfigurationRequest.builder()
                    .configurationToken(startConfigurationSessionResponse.initialConfigurationToken())
                    .build());
            String contentType = configurationResponse.contentType();
            data = configurationResponse.configuration().asByteArray();
            if (data.length == 0) {
                log.warn(
                        "Ignore the empty appconfig configuration and get it based on application[{}] " +
                                "& environment[{}] & profile[{}]",
                        application, environment, profile);
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("Loading appconfig data, application:{}, environment:{}, profile:{}, data:{} ", application,
                        environment, profile, data);
            }

            List<PropertySource<?>> propertySources = this.parseAppConfigData(profile, data, contentType);
            return Pair.of(configurationResponse.nextPollConfigurationToken(), propertySources);
        } catch (Exception e) {
            log.error("parse data from appconfig error, application:{}, environment:{}, profile:{}", application,
                    environment, profile, e);
        }
        return null;
    }


    /**
     * Parsing appconfig configuration content.
     *
     * @param profile     name of appconfig-config
     * @param configValue value from appconfig-config
     * @param contentType identifies the contentType of configValue
     * @return result of Map
     * @throws IOException thrown if there is a problem parsing config.
     */
    private List<PropertySource<?>> parseAppConfigData(String profile, byte[] configValue,
                                                      String contentType) throws IOException {
        if (configValue.length == 0) {
            return Collections.emptyList();
        }

        List<PropertySourceLoader> propertySourceLoaders = SpringFactoriesLoader
                .loadFactories(PropertySourceLoader.class, getClass().getClassLoader());
        for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
            if (Arrays.stream(propertySourceLoader.getFileExtensions()).noneMatch(contentType::contains)) {
                continue;
            }
            ByteArrayResource resource = new ByteArrayResource(configValue, profile);
            List<PropertySource<?>> propertySourceList = propertySourceLoader
                    .load(profile, resource);
            if (CollectionUtils.isEmpty(propertySourceList)) {
                return Collections.emptyList();
            }
            return propertySourceList.stream().filter(Objects::nonNull)
                    .map(propertySource -> {
                        if (propertySource instanceof EnumerablePropertySource) {
                            String[] propertyNames = ((EnumerablePropertySource<?>) propertySource)
                                    .getPropertyNames();
                            if (propertyNames.length > 0) {
                                Map<String, Object> map = new LinkedHashMap<>();
                                Arrays.stream(propertyNames).forEach(name
                                        -> map.put(name, propertySource.getProperty(name)));
                                return new OriginTrackedMapPropertySource(
                                        propertySource.getName(), map, true);
                            }
                        }
                        return propertySource;
                    }).toList();
        }
        return Collections.emptyList();
    }


    /**
     * Detect if configuration has changed
     * If there is a change, return the changed content*
     * @param application application
     * @param environment environment
     * @param profile profile
     * @return changed content
     */
    public String detectChanges(String application, String environment, String profile) {
        AppConfigPropertySource configPropertySource = AppConfigPropertySourceRepository.getAppConfigPropertySource(application, environment, profile);
        String token = configPropertySource.getNextToken();
        int maxRetry = 3;
        while (maxRetry > 0) {
            try {
                GetLatestConfigurationResponse configurationResponse = appConfigDataClient.getLatestConfiguration(GetLatestConfigurationRequest.builder()
                        .configurationToken(token)
                        .build());

                byte[] bytes = configurationResponse.configuration().asByteArray();
                String content = configurationResponse.configuration().asUtf8String();
                String contentType = configurationResponse.contentType();
                String nextPollConfigurationToken = configurationResponse.nextPollConfigurationToken();
                if (StringUtils.hasLength(content)) {
                    List<PropertySource<?>> propertySources = this.parseAppConfigData(profile, bytes, contentType);

                    AppConfigPropertySource appConfigPropertySource = new AppConfigPropertySource(propertySources, application,
                            environment, profile, nextPollConfigurationToken, new Date(), true);
                    AppConfigPropertySourceRepository.updateAppConfigPropertySource(appConfigPropertySource);
                    log.debug("config update, application:{}, environment:{}, profile:{}", application, environment, profile);
                } else {
                    log.debug("config no changed, application:{}, environment:{}, profile:{}", application, environment, profile);
                    configPropertySource.setNextToken(nextPollConfigurationToken);
                    AppConfigPropertySourceRepository.updateAppConfigPropertySource(configPropertySource);
                }
                return content;

            } catch (Exception e) {
                log.error("config refresh error, application:{}, environment:{}, profile:{}", application, environment, profile, e);
                StartConfigurationSessionResponse startConfigurationSessionResponse = appConfigDataClient
                        .startConfigurationSession(builder -> builder.applicationIdentifier(application)
                                .environmentIdentifier(environment)
                                .configurationProfileIdentifier(profile));
                token = startConfigurationSessionResponse.initialConfigurationToken();

                maxRetry--;
            }
        }

        return null;

    }


}
