package com.professionalloan.management.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ Handle UserNotFoundException
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFound(UserNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    // ✅ Handle LoanApplicationNotFoundException
    @ExceptionHandler(LoanApplicationNotFoundException.class)
    public ResponseEntity<?> handleLoanApplicationNotFound(LoanApplicationNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    // ✅ Handle DocumentNotFoundException
    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<?> handleDocumentNotFound(DocumentNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    // ✅ Handle DuplicateLoanApplicationException
    @ExceptionHandler(DuplicateLoanApplicationException.class)
    public ResponseEntity<?> handleDuplicateLoanApplication(DuplicateLoanApplicationException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT, request);
    }

    // ✅ Handle InvalidDocumentTypeException
    @ExceptionHandler(InvalidDocumentTypeException.class)
    public ResponseEntity<?> handleInvalidDocumentType(InvalidDocumentTypeException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    // ✅ Handle EMIAlreadyPaidException
    @ExceptionHandler(EMIAlreadyPaidException.class)
    public ResponseEntity<?> handleEMIAlreadyPaid(EMIAlreadyPaidException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT, request);
    }

    // ✅ Handle LoanAlreadyClosedException
    @ExceptionHandler(LoanAlreadyClosedException.class)
    public ResponseEntity<?> handleLoanAlreadyClosed(LoanAlreadyClosedException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT, request);
    }

    // ✅ Handle other RuntimeExceptions
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    // ✅ Handle any other Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    // ✅ Common method to build error response
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, status);
    }
}
