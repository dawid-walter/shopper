package com.shopper.aggregator.presentation.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.shopper.aggregator.domain.CatalogUnavailableException;
import com.shopper.aggregator.domain.InvalidIdentifierException;
import com.shopper.aggregator.domain.ProductNotFoundException;
import com.shopper.aggregator.domain.UnsupportedMarketException;

@RestControllerAdvice
class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    @ExceptionHandler({UnsupportedMarketException.class, InvalidIdentifierException.class})
    ProblemDetail badRequest(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail notFound(ProductNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail unavailable(CatalogUnavailableException e) {
        log.warn("Answering 503: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}
