package learning_exchange_platform.exception;


import learning_exchange_platform.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.sql.SQLException;

/**
 * 全局异常处理器
 */
@ControllerAdvice
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("参数校验异常：{}", e.getMessage());
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errorMsg = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMsg.append(fieldError.getField())
                    .append(": ")
                    .append(fieldError.getDefaultMessage())
                    .append("; ");
        }
        return Result.error("方法参数校验异常");
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("参数类型不匹配：{}", e.getMessage(), e);
        String msg = String.format("参数 '%s' 类型错误，期望类型：%s",
                e.getName(), e.getRequiredType().getSimpleName());
        return Result.error(msg);
    }

    /**
     * 处理缺少必要参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("缺少必要参数：{}", e.getMessage());
        String msg = String.format("缺少必要参数：%s", e.getParameterName());
        return Result.error(msg);
    }

    /**
     * 处理JSON解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("JSON解析异常：{}", e.getMessage());
        return Result.error("JSON解析异常：" + e.getMessage());
    }

    /**
     * 处理数据重复异常（如唯一约束冲突）
     */
    @ExceptionHandler(SQLException.class)
    public Result handleDuplicateKeyException(SQLException e) {
        log.error("数据库操作异常：{}", e.getMessage());
        return Result.error("数据库操作异常：" + e.getMessage());
    }

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result handleAccessDeniedException(AccessDeniedException e) {
        log.error("权限不足异常：{}", e.getMessage());
        return Result.error("权限不足异常：" + e.getMessage());
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.error("接口不存在：{} {}", e.getHttpMethod(), e.getRequestURL());
        String msg = String.format("接口不存在：%s %s", e.getHttpMethod(), e.getRequestURL());
        return Result.error(msg);
    }

    /**
     * 处理其他所有未分类异常
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常：", e.getMessage());
        e.printStackTrace();
        return Result.error("系统内部错误，请联系管理员");
    }
}
