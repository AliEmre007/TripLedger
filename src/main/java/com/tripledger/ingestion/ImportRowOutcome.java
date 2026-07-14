package com.tripledger.ingestion;

public enum ImportRowOutcome {
    ACCEPTED,
    DUPLICATE,
    REJECTED,
    FAILED
}
