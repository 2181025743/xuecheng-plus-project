package com.xuecheng.base.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 * 允许前端页面访问后端API
 * 
 * 实现方案二：在服务端添加CORS过滤器
 * 通过Spring Web MVC框架提供的CORS过滤器来解决跨域问题
 * 
 * 配置说明：
 * 1. 允许所有来源访问（开发环境）
 * 2. 允许所有HTTP方法（GET, POST, PUT, DELETE等）
 * 3. 允许所有请求头
 * 4. 允许发送Cookie
 * 5. 应用到所有请求路径
 * 
 * 注意：
 * 
 * @ConditionalOnWebApplication 表示只在 Web 应用环境中加载此配置
 *                              这样在单元测试等非 Web 环境中不会加载，避免 ClassNotFoundException
 */
@Configuration
@ConditionalOnWebApplication
public class CorsConfig {

    /**
     * 创建CORS过滤器Bean
     * 该过滤器会在HTTP响应头中添加跨域访问的相关信息
     * 
     * @return CorsFilter 跨域过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        // 创建CORS配置对象
        CorsConfiguration config = new CorsConfiguration();

        // 允许所有域名访问（开发环境）
        // 生产环境建议指定具体的域名，如：config.addAllowedOrigin("http://localhost:3000")
        config.addAllowedOrigin("*");

        // 允许所有请求头
        // 包括自定义的请求头，如：Authorization, Content-Type等
        config.addAllowedHeader("*");

        // 允许所有请求方法
        // 包括：GET, POST, PUT, DELETE, OPTIONS等
        config.addAllowedMethod("*");

        // 允许发送Cookie
        // 当需要在前端请求中携带Cookie时，必须设置为true
        config.setAllowCredentials(true);

        // 设置预检请求的缓存时间（秒）
        // 浏览器在发送实际请求前会先发送OPTIONS预检请求
        // 设置缓存时间可以减少预检请求的次数
        config.setMaxAge(3600L);

        // 创建基于URL的CORS配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 将CORS配置应用到所有路径（/**表示所有请求路径）
        source.registerCorsConfiguration("/**", config);

        // 返回CORS过滤器
        return new CorsFilter(source);
    }
}
