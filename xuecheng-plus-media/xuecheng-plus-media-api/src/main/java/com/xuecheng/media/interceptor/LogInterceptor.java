package com.xuecheng.media.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * 日志追踪拦截器
 * 
 * 🎯 核心功能：
 * 1. 为每个HTTP请求生成唯一的requestId
 * 2. 使用MDC在整个请求链路中传递requestId
 * 3. 记录请求的详细信息（URI、方法、IP、耗时等）
 * 4. 清理MDC，避免内存泄漏
 * 
 * 📊 工作原理：
 * - MDC（Mapped Diagnostic Context）是SLF4J提供的一种机制
 * - 可以在当前线程中存储键值对
 * - 在日志输出时，可以通过 %X{key} 获取MDC中的值
 * - 非常适合在分布式系统中追踪请求
 * 
 * 🔍 使用方式：
 * - 在日志配置中使用：%X{requestId}、%X{userId}、%X{ip}
 * - 在业务代码中可以直接使用：MDC.get("requestId")
 * 
 * ⚠️ 注意事项：
 * - 必须在finally块中清理MDC，否则会导致内存泄漏
 * - 在异步线程中需要手动传递MDC
 * 
 * @author AI Assistant
 * @version 1.0
 * @date 2025-10-07
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    // MDC键名常量
    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String IP = "ip";
    private static final String START_TIME = "startTime";

    /**
     * 请求处理之前调用
     * 
     * 工作内容：
     * 1. 生成并设置requestId
     * 2. 获取并设置用户信息
     * 3. 获取并设置客户端IP
     * 4. 记录请求开始时间
     * 5. 记录请求的基本信息
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // ===== 生成请求ID（16位，便于阅读） =====
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(REQUEST_ID, requestId);
        
        // ===== 获取客户端IP =====
        String ip = getClientIP(request);
        MDC.put(IP, ip);
        
        // ===== 获取用户ID（从请求头或session中获取） =====
        String userId = request.getHeader("userId");
        if (userId != null && !userId.isEmpty()) {
            MDC.put(USER_ID, userId);
        } else {
            MDC.put(USER_ID, "anonymous");
        }
        
        // ===== 记录请求开始时间 =====
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);
        
        // ===== 记录请求信息 =====
        log.info("========== HTTP请求开始 ==========");
        log.info("RequestID: {}", requestId);
        log.info("请求URI: {} {}", request.getMethod(), request.getRequestURI());
        if (request.getQueryString() != null) {
            log.info("Query参数: {}", request.getQueryString());
        }
        log.info("客户端IP: {}", ip);
        log.debug("User-Agent: {}", request.getHeader("User-Agent"));
        log.debug("Content-Type: {}", request.getContentType());
        
        return true;
    }

    /**
     * 请求处理完成后调用（无论成功还是异常）
     * 
     * 工作内容：
     * 1. 计算请求执行时间
     * 2. 记录响应状态
     * 3. 记录慢请求告警
     * 4. 记录异常信息
     * 5. 清理MDC
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        try {
            // ===== 计算执行时间 =====
            Long startTime = (Long) request.getAttribute(START_TIME);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                
                log.info("========== HTTP请求结束 ==========");
                log.info("响应状态: {}", response.getStatus());
                log.info("执行耗时: {}ms", duration);
                
                // ===== 慢请求告警（超过3秒） =====
                if (duration > 3000) {
                    log.warn("⚠️⚠️⚠️ 慢请求告警！URI: {} {}, 耗时: {}ms", 
                            request.getMethod(), request.getRequestURI(), duration);
                }
                
                // ===== 超慢请求严重告警（超过10秒） =====
                if (duration > 10000) {
                    log.error("🔥🔥🔥 超慢请求严重告警！URI: {} {}, 耗时: {}ms", 
                            request.getMethod(), request.getRequestURI(), duration);
                }
            }
            
            // ===== 记录异常 =====
            if (ex != null) {
                log.error("❌ 请求执行异常：URI: {} {}", request.getMethod(), request.getRequestURI(), ex);
            }
            
        } finally {
            // ===== 清理MDC（非常重要！避免内存泄漏和线程复用时的数据污染） =====
            // Tomcat线程池会复用线程，如果不清理MDC，下一个请求可能会使用上一个请求的数据
            MDC.clear();
        }
    }

    /**
     * 获取客户端真实IP
     * 
     * 📝 背景说明：
     * - 当请求经过nginx等反向代理时，request.getRemoteAddr()获取的是代理服务器的IP
     * - 真实的客户端IP会存储在X-Forwarded-For或X-Real-IP请求头中
     * - 需要按优先级依次尝试获取
     * 
     * 🔍 获取优先级：
     * 1. X-Forwarded-For（标准头，多级代理时第一个IP是客户端IP）
     * 2. X-Real-IP（nginx常用）
     * 3. Proxy-Client-IP（Apache常用）
     * 4. WL-Proxy-Client-IP（WebLogic常用）
     * 5. RemoteAddr（直连时的IP）
     * 
     * @param request HTTP请求对象
     * @return 客户端真实IP地址
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多级代理的情况
        // X-Forwarded-For格式：客户端IP, 代理1IP, 代理2IP, ...
        // 取第一个IP即为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}

