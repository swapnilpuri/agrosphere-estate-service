package com.agrosphere.estate.adapter.in.web;
import com.agrosphere.estate.domain.DomainException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.*;
@RestControllerAdvice
public class ApiErrors {
    private static final Logger log=LoggerFactory.getLogger(ApiErrors.class);
    private ProblemDetail problem(int status,String detail) {
        var p=ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status),detail);
        p.setProperty("correlationId",CorrelationFilter.current().toString()); return p;
    }
    @ExceptionHandler(DomainException.class) ProblemDetail domain(DomainException ex) { return problem(switch(ex.kind()){case INVALID->400;case NOT_FOUND->404;case CONFLICT->409;},ex.getMessage()); }
    @ExceptionHandler({DataIntegrityViolationException.class,ObjectOptimisticLockingFailureException.class,OptimisticLockException.class})
    ProblemDetail conflict(Exception ex) { return problem(409,"A unique code, relationship or version constraint was violated"); }
    @ExceptionHandler({MethodArgumentNotValidException.class,MethodArgumentTypeMismatchException.class,HttpMessageNotReadableException.class,jakarta.validation.ConstraintViolationException.class})
    ProblemDetail invalid(Exception ex) { return problem(400,"Invalid request fields or values"); }
    @ExceptionHandler(AccessDeniedException.class) ProblemDetail denied(Exception ex) { return problem(403,"Estate access denied"); }
    @ExceptionHandler(org.springframework.web.ErrorResponseException.class) ProblemDetail web(org.springframework.web.ErrorResponseException ex) { return problem(ex.getStatusCode().value(),ex.getStatusCode().toString()); }
    @ExceptionHandler(Exception.class) ProblemDetail unknown(Exception ex) { if(ex instanceof org.springframework.web.ErrorResponse response) return problem(response.getStatusCode().value(),response.getStatusCode().toString()); log.error("Request failed: {}",ex.getClass().getSimpleName()); return problem(500,"Unexpected service error"); }
}
