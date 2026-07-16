package com.tripledger.operations;

public class TransientJobException extends RuntimeException {

    private final String diagnosticCategory;

    public TransientJobException(String diagnosticCategory, String message) {
        super(message);
        this.diagnosticCategory = diagnosticCategory;
    }

    public String diagnosticCategory() {
        return diagnosticCategory;
    }
}
