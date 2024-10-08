package com.dong.common.configs.config;

import javax.validation.constraints.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolExecutorConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 1;

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程" + count);
                count++;
                return thread;
            }
        };
//        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 20, 120, TimeUnit.SECONDS,
//                new ArrayBlockingQueue<>(100), threadFactory, new ThreadPoolExecutor.DiscardPolicy());

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 20, 120, TimeUnit.SECONDS,
                new ResizableCapacityLinkedBlockingQueue<>(100), threadFactory, new ThreadPoolExecutor.DiscardPolicy());

        threadPoolExecutor.prestartAllCoreThreads();

        return threadPoolExecutor;

    }

}
