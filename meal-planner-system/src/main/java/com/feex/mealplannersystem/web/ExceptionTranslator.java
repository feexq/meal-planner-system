package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.service.exception.*;
import com.feex.mealplannersystem.web.exception.ParamsViolationDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

import static com.feex.mealplannersystem.util.ValidationDetailsUtils.getValidationErrorsProblemDetail;
import static java.net.URI.create;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.ProblemDetail.forStatusAndDetail;

@Slf4j
@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RefreshTokenExpiredException.class)
    ProblemDetail handleRefreshTokenExpiredException(RefreshTokenExpiredException ex) {
        log.error("RefreshTokenExpiredException");
        ProblemDetail problemDetail = forStatusAndDetail(CONFLICT, ex.getMessage());
        problemDetail.setStatus(CONFLICT);
        problemDetail.setType(create("refresh-token-expired-exception"));
        problemDetail.setTitle("Refresh Token Expired Exception");
        return problemDetail;
    }

    @ExceptionHandler(IngredientNotAvailableException.class)
    ProblemDetail handleIngredientNotAvailableException(IngredientNotAvailableException ex) {
        log.error("IngredientNotAvailableException");
        ProblemDetail problemDetail = forStatusAndDetail(CONFLICT, ex.getMessage());
        problemDetail.setStatus(CONFLICT);
        problemDetail.setType(create("ingredient-not-available-exception"));
        problemDetail.setTitle("Ingredient Not Available Exception");
        return problemDetail;
    }

    @ExceptionHandler(CustomAlreadyExistsException.class)
    ProblemDetail handleCustomAlreadyExistsException(CustomAlreadyExistsException ex) {
        log.error("AlreadyExistsException");
        ProblemDetail problemDetail = forStatusAndDetail(CONFLICT, ex.getMessage());
        problemDetail.setStatus(CONFLICT);
        problemDetail.setType(create("already-exists-exception"));
        problemDetail.setTitle("Already Exists Exception");
        return problemDetail;
    }

    @ExceptionHandler(CategoryParentException.class)
    ProblemDetail handleCategoryParentException(CategoryParentException ex) {
        log.error("CategoryParentException");
        ProblemDetail problemDetail = forStatusAndDetail(CONFLICT, ex.getMessage());
        problemDetail.setStatus(CONFLICT);
        problemDetail.setType(create("category-parent-exception"));
        problemDetail.setTitle("Category Parent Exception");
        return problemDetail;
    }

    @ExceptionHandler(CustomNotFoundException.class)
    ProblemDetail handleCustomNotFoundException(CustomNotFoundException ex) {
        log.error("NotFoundException");
        ProblemDetail problemDetail = forStatusAndDetail(NOT_FOUND, ex.getMessage());
        problemDetail.setStatus(NOT_FOUND);
        problemDetail.setType(create("Not-found"));
        problemDetail.setTitle("Not Found");
        return problemDetail;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    ProblemDetail handelUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.error("UserAlreadyExistsException");
        ProblemDetail problemDetail = forStatusAndDetail(CONFLICT, ex.getMessage());
        problemDetail.setStatus(CONFLICT);
        problemDetail.setType(create("user-already-exists"));
        problemDetail.setTitle("User Already Exists");
        return problemDetail;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors();
        List<ParamsViolationDetails> validationResponse =
                errors.stream().map(err -> ParamsViolationDetails.builder().reason(err.getDefaultMessage()).fieldName(err.getField()).build()).toList();
        log.info("Input params validation failed");
        return ResponseEntity.status(BAD_REQUEST).body(getValidationErrorsProblemDetail(validationResponse));
    }
}
