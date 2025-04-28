package com.professionalloan.management.exception;

/**
 * Exception thrown when an EMI is already marked as paid.
 */
public class EMIAlreadyPaidException extends RuntimeException {
    public EMIAlreadyPaidException(String message) {
        super(message);
    }
}
