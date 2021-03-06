package org.infinity.passport.controller.advice;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.infinity.passport.config.ApplicationConstants;
import org.infinity.passport.dto.ParameterizedErrorDTO;
import org.infinity.passport.exception.CustomParameterizedException;
import org.infinity.passport.exception.FieldValidationException;
import org.infinity.passport.exception.LoginUserNotExistException;
import org.infinity.passport.exception.NoAuthorityException;
import org.infinity.passport.exception.NoDataException;
import org.infinity.passport.utils.HttpHeaderCreator;
import org.infinity.passport.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 */
@ControllerAdvice
public class ExceptionTranslatorAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionTranslatorAdvice.class);

    @Autowired
    private MessageSource       messageSource;

    @Autowired
    private HttpHeaderCreator   httpHeaderCreator;

    /**
     * JSR 303 Bean Validation Warn handler
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorDTO> processBeanValidationError(MethodArgumentNotValidException ex) {
        String warnMessage = messageSource.getMessage(ErrorCodeConstants.WARN_FIELDS_VALIDATION_ERROR, null,
                ApplicationConstants.SYSTEM_LOCALE);
        // warn级别记录用户输入错误，error级别只记录系统逻辑出错、异常、或者重要的错误信息
        LOGGER.warn(warnMessage);
        return ResponseEntity.badRequest()
                .headers(httpHeaderCreator.createWarnHeader(ErrorCodeConstants.WARN_FIELDS_VALIDATION_ERROR))
                .body(processFieldErrors(ex.getBindingResult().getFieldErrors()));
    }

    /**
     * Field Validation Warn handler
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public ResponseEntity<ErrorDTO> processFieldValidationError(BindException ex) {
        String warnMessage = messageSource.getMessage(ErrorCodeConstants.WARN_FIELDS_VALIDATION_ERROR, null,
                ApplicationConstants.SYSTEM_LOCALE);
        // warn级别记录用户输入错误，error级别只记录系统逻辑出错、异常、或者重要的错误信息
        LOGGER.warn(warnMessage);
        return ResponseEntity.badRequest()
                .headers(httpHeaderCreator.createWarnHeader(ErrorCodeConstants.WARN_FIELDS_VALIDATION_ERROR))
                .body(processFieldErrors(ex.getBindingResult().getFieldErrors()));
    }

    /**
     * Field Validation Warn handler
     */
    @ExceptionHandler(FieldValidationException.class)
    @ResponseBody
    public ResponseEntity<ErrorDTO> processFieldValidationError(FieldValidationException ex) {
        String warnMessage = messageSource.getMessage(ErrorCodeConstants.WARN_FIELDS_VALIDATION_ERROR, null,
                ApplicationConstants.SYSTEM_LOCALE);
        // warn级别记录用户输入错误，error级别只记录系统逻辑出错、异常、或者重要的错误信息
        LOGGER.warn(warnMessage);
        return ResponseEntity.badRequest()
                .headers(httpHeaderCreator.createWarnHeader(ErrorCodeConstants.WARN_FIELDS_VALIDATION_ERROR))
                .body(processFieldErrors(ex.getFieldErrors()));
    }

    /**
     * Login user not exist error handler
     */
    @ExceptionHandler(LoginUserNotExistException.class)
    @ResponseBody
    public ResponseEntity<ParameterizedErrorDTO> processLoginUserNotExistError(LoginUserNotExistException ex) {
        String errorMessage = messageSource.getMessage(ErrorCodeConstants.ERROR_LOGIN_USER_NOT_EXIST,
                new Object[] { ex.getUserName() }, ApplicationConstants.SYSTEM_LOCALE);
        ex.setMessage(errorMessage);
        LogUtils.error(errorMessage);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR).headers(httpHeaderCreator
                        .createErrorHeader(ErrorCodeConstants.ERROR_LOGIN_USER_NOT_EXIST, ex.getUserName()))
                .body(ex.getErrorDTO());
    }

    /**
     * No authority error handler
     */
    @ExceptionHandler(NoAuthorityException.class)
    @ResponseBody
    public ResponseEntity<ParameterizedErrorDTO> processNoAuthorityError(NoAuthorityException ex) {
        String errorMessage = messageSource.getMessage(ErrorCodeConstants.ERROR_NO_AUTHORITIES,
                new Object[] { ex.getUserName() }, ApplicationConstants.SYSTEM_LOCALE);
        ex.setMessage(errorMessage);
        LogUtils.error(errorMessage);
        return ResponseEntity.badRequest()
                .headers(httpHeaderCreator.createErrorHeader(ErrorCodeConstants.ERROR_NO_AUTHORITIES, ex.getUserName()))
                .body(ex.getErrorDTO());
    }

    /**
     * No data error handler
     */
    @ExceptionHandler(NoDataException.class)
    @ResponseBody
    public ResponseEntity<ParameterizedErrorDTO> processDataNotExistError(NoDataException ex) {
        String errorMessage = messageSource.getMessage(ErrorCodeConstants.ERROR_DATA_NOT_EXIST,
                new Object[] { ex.getId() }, ApplicationConstants.SYSTEM_LOCALE);
        ex.setMessage(errorMessage);
        LogUtils.error(errorMessage);
        return ResponseEntity.badRequest()
                .headers(httpHeaderCreator.createErrorHeader(ErrorCodeConstants.ERROR_DATA_NOT_EXIST, ex.getId()))
                .body(ex.getErrorDTO());
    }

    /**
     * Custom Error handler
     */
    @ExceptionHandler(CustomParameterizedException.class)
    @ResponseBody
    public ResponseEntity<ParameterizedErrorDTO> processCustomParameterizedError(CustomParameterizedException ex) {
        LogUtils.error(ex.getMessage());
        return ResponseEntity.badRequest().headers(httpHeaderCreator.createErrorHeader(ex.getCode(), ex.getParams()))
                .body(ex.getErrorDTO());
    }

    /**
     * Spring security access denied handler
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<ErrorDTO> processAccessDeniedExcpetion(AccessDeniedException ex) {
        String warnMessage = messageSource.getMessage(ErrorCodeConstants.WARN_ACCESS_DENIED, null,
                ApplicationConstants.SYSTEM_LOCALE);
        LOGGER.warn(warnMessage);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .headers(httpHeaderCreator.createErrorHeader(ErrorCodeConstants.WARN_ACCESS_DENIED))
                .body(new ErrorDTO(ErrorCodeConstants.WARN_ACCESS_DENIED, warnMessage));
    }

    /**
     * Method not supported handler，这个方法好像无法调用到
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public ResponseEntity<ErrorDTO> processMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        String warnMessage = messageSource.getMessage(ErrorCodeConstants.WARN_METHOD_NOT_SUPPORTED, null,
                ApplicationConstants.SYSTEM_LOCALE);
        LOGGER.warn(warnMessage);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(httpHeaderCreator.createErrorHeader(ErrorCodeConstants.WARN_METHOD_NOT_SUPPORTED))
                .body(new ErrorDTO(ErrorCodeConstants.WARN_METHOD_NOT_SUPPORTED, warnMessage));
    }

    /**
     * Concurrency failure handler
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    @ResponseBody
    public ResponseEntity<ErrorDTO> processConcurencyException(ConcurrencyFailureException ex) {
        String errorMessage = messageSource.getMessage(ErrorCodeConstants.ERROR_CONCURRENCY_EXCEPTION, null,
                ApplicationConstants.SYSTEM_LOCALE);
        LogUtils.error(ex, errorMessage);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .headers(httpHeaderCreator.createErrorHeader(ErrorCodeConstants.ERROR_CONCURRENCY_EXCEPTION))
                .body(new ErrorDTO(ErrorCodeConstants.ERROR_CONCURRENCY_EXCEPTION, errorMessage));
    }

    /**
     * Exception handler
     */
    @ExceptionHandler(Throwable.class)
    @ResponseBody
    public ResponseEntity<ErrorDTO> processException(Throwable throwable) {
        String errorMessage = messageSource.getMessage(ErrorCodeConstants.ERROR_SYSTEM_EXCEPTION, null,
                ApplicationConstants.SYSTEM_LOCALE);
        LogUtils.error(throwable, errorMessage);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .headers(httpHeaderCreator.createErrorHeader(ErrorCodeConstants.ERROR_SYSTEM_EXCEPTION))
                .body(new ErrorDTO(ErrorCodeConstants.ERROR_SYSTEM_EXCEPTION, errorMessage));
    }

    private ErrorDTO processFieldErrors(List<FieldError> fieldErrors) {
        List<FieldError> newFieldErrors = new ArrayList<FieldError>();
        for (FieldError fieldError : fieldErrors) {
            String defaultMessage = StringUtils.isEmpty(fieldError.getDefaultMessage())
                    ? messageSource.getMessage(fieldError.getCodes()[0], fieldError.getArguments(),
                            ApplicationConstants.SYSTEM_LOCALE)
                    : fieldError.getDefaultMessage();
            FieldError newFieldError = new FieldError(fieldError.getObjectName(), fieldError.getField(),
                    fieldError.getRejectedValue(), true, fieldError.getCodes(), fieldError.getArguments(),
                    defaultMessage);
            newFieldErrors.add(newFieldError);
        }
        ErrorDTO dto = new ErrorDTO(ErrorCodeConstants.WARN_FIELDS_VALIDATION_ERROR, messageSource
                .getMessage(ErrorCodeConstants.WARN_FIELDS_VALIDATION_ERROR, null, ApplicationConstants.SYSTEM_LOCALE),
                newFieldErrors);
        return dto;
    }
}
