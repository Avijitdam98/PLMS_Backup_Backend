package com.professionalloan.management.exception;

/**
 * Exception thrown when a document is not found.
 */
public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String message) {
        super(message);
    }
}
