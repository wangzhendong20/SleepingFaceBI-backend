package com.dong.common.configs.config;

import com.dong.common.excption.BusinessException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SpringRetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        Map<Class<? extends Throwable>, Boolean> exceptionMap = new HashMap<>();
        exceptionMap.put(BusinessException.class, true);
        RetryTemplate retryTemplate = new RetryTemplate();
        // 重试策略，参数依次代表：最大尝试次数、可重试的异常映射
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, exceptionMap);
        retryTemplate.setRetryPolicy(retryPolicy);
        // 重试间隔时间
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(5000L);
        backOffPolicy.setMultiplier(1.5);
        backOffPolicy.setMaxInterval(60000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}
