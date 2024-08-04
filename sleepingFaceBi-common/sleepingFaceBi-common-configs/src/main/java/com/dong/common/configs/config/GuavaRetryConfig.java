package com.dong.common.configs.config;

import com.dong.common.excption.BusinessException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class GuavaRetryConfig {

    @Bean
    public Retryer<String> retryer() {
        return RetryerBuilder.<String>newBuilder()
                .retryIfResult(Predicates.isNull()) // 如果结果为空则重试
                .retryIfExceptionOfType(BusinessException.class) // 发生自定义异常则重试
//                .retryIfRuntimeException() // 发生运行时异常则重试
                .withWaitStrategy(WaitStrategies.incrementingWait(10, TimeUnit.SECONDS, 60, TimeUnit.SECONDS)) // 等待
                .withStopStrategy(StopStrategies.stopAfterAttempt(4)) // 允许执行4次（首次执行 + 最多重试3次）
                .build();
    }
}
