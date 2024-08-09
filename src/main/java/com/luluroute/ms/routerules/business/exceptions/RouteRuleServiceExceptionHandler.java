package com.luluroute.ms.routerules.business.exceptions;


import com.luluroute.ms.routerules.business.util.ServiceStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroMissingFieldException;
import org.apache.avro.AvroRuntimeException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Set;
import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class RouteRuleServiceExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecordNotFoundException.class)
    protected ResponseEntity<Object> handleRecordNotFoundException(RecordNotFoundException ex) {
        log.info("Record was not found in database. RecordNotFoundException exception was thrown.", ex);
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_404);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_404.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.info("Unable to execute data query. DataIntegrityViolationException exception was thrown.", ex);
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_400);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_400.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RouteRuleServiceException.class)
    protected ResponseEntity<Object> handleEntityServiceException(RouteRuleServiceException ex) {
        log.error("An internal error occurred. RouteRuleServiceException exception was thrown.", ex);
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_999);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_999.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidEntityException.class)
    protected ResponseEntity<Object> handleInvalidEntityException(InvalidEntityException ex) {
        log.error("Entity service - request payload is invalid. InvalidEntityException exception was thrown.", ex);
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_999);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_999.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<Object> handleDataAccessException(DataAccessException ex) {
        log.error("An internal error occurred. DataAccessException exception was thrown.", ex);
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_999);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_999.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Collect the validation errors
     */
    @Override
    @NotNull
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, @NotNull HttpHeaders headers, @NotNull HttpStatus status,
            @NotNull WebRequest request) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(x -> "%s: %s".formatted(x.getField(), x.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        String message = String.format("Invalid inbound request method argument(s). [%s]", errors);
        return super.handleExceptionInternal(ex, message, headers, status, request);

    }

    /**
     * Generic exception handler
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleUnhandledException(Exception ex) {
        log.error("An internal error occurred. An unhandled exception was thrown. ", ex);
        return new ResponseEntity<>(ex.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AvroMissingFieldException.class)
    protected ResponseEntity<Object> handleAvroMissingFieldException(AvroMissingFieldException ex) {
        log.error("An internal error occurred. AvroMissingFieldException exception was thrown.", ex);
        return new ResponseEntity<>(ExceptionUtils.getStackTrace(ex), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AvroRuntimeException.class)
    protected ResponseEntity<Object> handleAvroRuntimeException(AvroRuntimeException ex) {
        log.error("An internal error occurred. AvroRuntimeException exception was thrown.", ex);
        return new ResponseEntity<>(ExceptionUtils.getStackTrace(ex), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Override super to return error in response body
     */
    @Override
    @NotNull
    public ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, @NotNull HttpHeaders headers, @NotNull HttpStatus status,
            @NotNull WebRequest request) {
        pageNotFoundLogger.error(ex.getMessage());
        Set<HttpMethod> supportedMethods = ex.getSupportedHttpMethods();
        if (!CollectionUtils.isEmpty(supportedMethods)) {
            headers.setAllow(supportedMethods);
        }

        return this.handleExceptionInternal(ex, ex.getMessage(), headers, status, request);
    }

    /**
     * Override super to return error in response body
     */
    @Override
    @NotNull
    public ResponseEntity<Object> handleHttpMessageNotReadable(
            @NotNull HttpMessageNotReadableException ex, @NotNull HttpHeaders headers, @NotNull HttpStatus status,
            @NotNull WebRequest request) {
        return this.handleExceptionInternal(ex, ex.getMessage(), headers, status, request);
    }

    /**
     * Override super to return error in response body
     */
    @Override
    @NotNull
    public ResponseEntity<Object> handleMissingServletRequestParameter(
            @NotNull MissingServletRequestParameterException ex, @NotNull HttpHeaders headers, @NotNull HttpStatus status,
            @NotNull WebRequest request) {
        return this.handleExceptionInternal(ex, ex.getMessage(), headers, status, request);
    }

    /**
     * Override super to include a log statement on all handling
     */
    @Override
    public @NotNull ResponseEntity<Object> handleExceptionInternal(
            @NotNull Exception ex, @Nullable Object body, @NotNull HttpHeaders headers, @NotNull HttpStatus status,
            @NotNull WebRequest request) {
        log.error("Global exception handler handling: ", ex);
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }
}