package com.narrativeplatform;

import com.narrativeplatform.configuration.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class NarrativePlatformApplication {

    public static void main(final String[] args) {
        SpringApplication.run(NarrativePlatformApplication.class, args);
    }
}
