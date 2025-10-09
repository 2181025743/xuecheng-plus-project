package com.xuecheng.media.config;

import com.xuecheng.media.interceptor.LogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 
 * 🎯 功能说明：
 * 配置Spring MVC的各种组件，包括拦截器、跨域、静态资源等
 * 
 * 📊 当前配置内容：
 * 1. 注册日志追踪拦截器
 * 
 * @author AI Assistant
 * @version 1.0
 * @date 2025-10-07
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LogInterceptor logInterceptor;

    /**
     * 注册拦截器
     * 
     * 配置说明：
     * - addPathPatterns("/**")：拦截所有请求
     * - excludePathPatterns：排除不需要拦截的路径
     * - /error：Spring Boot默认错误页面
     * - /actuator/**：健康检查、监控端点
     * - /swagger-ui/**：Swagger UI界面
     * - /v2/api-docs：Swagger API文档
     * - /webjars/**：Swagger静态资源
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/v3/api-docs",
                        "/webjars/**");
    }
}
