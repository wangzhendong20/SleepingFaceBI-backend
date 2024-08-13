package com.dong.text.config;

import com.dong.apiclientsdk.client.ApiClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "api.client")
public class ApiClientConfig {

    private String accessKey;

    private String secretKey;

    @Bean
    public ApiClient apiClient() {
        return new ApiClient(accessKey, secretKey);
    }

}
