package com.tripledger.authorization;

public enum Permission {
    PROTECTED_READ,
    SOURCE_SYSTEM_MANAGE,
    OPERATIONAL_WRITE,
    FINANCIAL_ACTION,
    FINANCIAL_ACTION_WITH_MFA
}
