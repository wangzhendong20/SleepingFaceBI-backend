package com.dong.gateway.filter;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.dong.gateway.config.properties.IgnoreWhiteProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;


@Configuration
public class AuthFilter {

    @Bean
    public SaReactorFilter getSaReactorFilter(IgnoreWhiteProperties ignoreWhite) {
        return new SaReactorFilter()
                .addInclude("/**")
                .addExclude("/favicon.ico", "/actuator/**")
                .setAuth(obj -> {
                    SaRouter.match("/**")
                            .notMatch(ignoreWhite.getWhites())
                            .check(r -> {
                                // check if logined
                                StpUtil.checkLogin();
                            });
                }).setError(e -> {
                    System.out.println(e);
                    return SaResult.error("Unauthorized, you cannot access system resources").setCode(HttpStatus.UNAUTHORIZED.value());
                });
    }
}
