package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理器
 * <p>
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
     * 处理JSR 303参数校验异常
     * <p>
     * 场景：使用@Validated注解校验失败时抛出
     * 示例：课程名称为空、适用人群内容过少等
     *
     * @param e 参数校验异常
     * @return 统一错误响应（包含所有校验失败的字段信息）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400状态码（客户端参数错误）
    public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e) {

        // 1. 获取校验结果
        BindingResult bindingResult = e.getBindingResult();

        // 2. 提取所有字段的错误信息
        List<String> errors = new ArrayList<>();
        bindingResult.getFieldErrors().forEach(error -> {
            // error.getDefaultMessage() 获取注解中的message属性值
            errors.add(error.getDefaultMessage());
        });

        // 3. 将多个错误信息拼接成一个字符串（用逗号分隔）
        // 示例："课程名称不能为空, 适用人群内容过少"
        String errMessage = StringUtils.join(errors, ", ");

        // 4. 记录日志
        log.error("【参数校验异常】{}", errMessage);

        // 5. 返回友好的错误提示
        return new RestErrorResponse(errMessage);
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
