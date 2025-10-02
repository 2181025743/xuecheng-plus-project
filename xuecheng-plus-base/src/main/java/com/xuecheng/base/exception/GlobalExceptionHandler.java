package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 全局异常处理器
 * 
 * 功能：统一捕获和处理所有Controller抛出的异常
 * 原理：基于AOP的@ControllerAdvice增强Controller
 */
@Slf4j
@ControllerAdvice // 或使用 @RestControllerAdvice = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    /**
     * 处理自定义异常（可预知的业务异常）
     * 
     * @param e 自定义异常对象
     * @return 统一错误响应（返回具体的业务错误信息）
     */
    @ExceptionHandler(XuechengPlusException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(XuechengPlusException e) {
        // 记录异常日志
        log.error("【系统异常】{}", e.getErrMessage(), e);

        // 返回具体的业务错误信息
        return new RestErrorResponse(e.getErrMessage());
    }

    /**
     * 处理其他所有异常（不可预知的系统异常）
     * 
     * @param e 异常对象
     * @return 统一错误响应（返回通用提示，不暴露技术细节）
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse exception(Exception e) {
        // 记录详细异常日志（方便排查问题）
        log.error("【系统异常】{}", e.getMessage(), e);

        // 返回通用错误提示（不暴露技术细节给用户）
        return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
    }
}
