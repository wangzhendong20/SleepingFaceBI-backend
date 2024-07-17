package com.dong.chart;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author dong
 * @version 1.0
 * @project sleepingFaceBi-cloud
 * @description chart启动类
 * @date 2023/7/25 21:33:13
 */
@SpringBootApplication
@EnableDubbo
@Slf4j
@MapperScan("com.dong.chart.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class ChartMainApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChartMainApplication.class,args);
    }
}
