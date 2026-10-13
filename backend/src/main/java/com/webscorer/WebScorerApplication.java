package com.webscorer;

import com.webscorer.config.ScanProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ScanProperties.class)
public class WebScorerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebScorerApplication.class, args);
    }
}
