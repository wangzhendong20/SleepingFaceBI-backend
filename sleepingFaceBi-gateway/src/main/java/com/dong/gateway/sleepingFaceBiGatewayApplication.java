package com.dong.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 网关 8099
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class sleepingFaceBiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(sleepingFaceBiGatewayApplication.class, args);
    }
}
