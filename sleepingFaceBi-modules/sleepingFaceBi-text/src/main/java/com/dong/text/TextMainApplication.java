package com.dong.text;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;



// todo 如需开启 Redis，须移除 exclude 中的内容
@SpringBootApplication
@EnableDubbo
@Slf4j
@MapperScan("com.dong.text.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class TextMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(TextMainApplication.class, args);
    }
}
