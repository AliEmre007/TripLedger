package com.tripledger.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyPolicyTest {

    @Test
    void normalizesSupportedCurrencyAmountsWithExactDecimals() {
        assertThat(MoneyPolicy.positiveAmount("10", "eur")).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(MoneyPolicy.nonNegativeAmount("0", "USD")).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void rejectsUnsupportedCurrency() {
        assertThatThrownBy(() -> MoneyPolicy.currency("XXX"))
                .isInstanceOf(MoneyValidationException.class)
                .extracting("code")
                .isEqualTo("INVALID_CURRENCY");
    }

    @Test
    void rejectsInvalidCurrencyPrecision() {
        assertThatThrownBy(() -> MoneyPolicy.positiveAmount("10.001", "EUR"))
                .isInstanceOf(MoneyValidationException.class)
                .extracting("code")
                .isEqualTo("INVALID_CURRENCY_PRECISION");
    }

    @Test
    void rejectsNegativeAmountsUnlessCallerUsesExplicitCorrectionPath() {
        assertThatThrownBy(() -> MoneyPolicy.nonNegativeAmount("-0.01", "EUR"))
                .isInstanceOf(MoneyValidationException.class)
                .extracting("code")
                .isEqualTo("INVALID_FIELD_TYPE");
        assertThatThrownBy(() -> MoneyPolicy.positiveAmount("0", "EUR"))
                .isInstanceOf(MoneyValidationException.class)
                .extracting("reason")
                .isEqualTo("Amount must be positive.");
    }

    @Test
    void convertsUsingExactDecimalRateAndTargetCurrencyRounding() {
        assertThat(MoneyPolicy.convert(
                new BigDecimal("3500.00"),
                "TRY",
                "EUR",
                new BigDecimal("0.0285714286")))
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
