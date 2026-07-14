package com.tripledger.common.money;

public class MoneyValidationException extends RuntimeException {

    private final String code;
    private final String reason;

    MoneyValidationException(String code, String reason) {
        super(reason);
        this.code = code;
        this.reason = reason;
    }

    public String code() {
        return code;
    }

    public String reason() {
        return reason;
    }
}
