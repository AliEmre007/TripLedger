package com.tripledger.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class MoneyPolicy {

    private static final Map<String, Integer> FRACTION_DIGITS = Map.of(
            "EUR", 2,
            "GBP", 2,
            "TRY", 2,
            "USD", 2
    );

    private MoneyPolicy() {
    }

    public static Set<String> supportedCurrencies() {
        return FRACTION_DIGITS.keySet();
    }

    public static BigDecimal convert(BigDecimal sourceAmount,
                                     String sourceCurrency,
                                     String targetCurrency,
                                     BigDecimal rate) {
        BigDecimal normalizedSourceAmount = positiveAmount(sourceAmount, sourceCurrency);
        String normalizedTargetCurrency = currency(targetCurrency);
        if (rate == null || rate.signum() <= 0) {
            throw new MoneyValidationException("INVALID_FIELD_TYPE", "Rate must be positive.");
        }
        int targetScale = FRACTION_DIGITS.get(normalizedTargetCurrency);
        return normalizedSourceAmount.multiply(rate).setScale(targetScale, RoundingMode.HALF_UP);
    }

    public static BigDecimal nonNegativeAmount(String rawAmount, String currency) {
        BigDecimal amount = decimal(rawAmount);
        if (amount.signum() < 0) {
            throw new MoneyValidationException("INVALID_FIELD_TYPE", "Amount must be non-negative.");
        }
        return normalize(amount, currency);
    }

    public static BigDecimal positiveAmount(String rawAmount, String currency) {
        return positiveAmount(decimal(rawAmount), currency);
    }

    public static BigDecimal positiveAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new MoneyValidationException("INVALID_FIELD_TYPE", "Amount must be a decimal number.");
        }
        if (amount.signum() <= 0) {
            throw new MoneyValidationException("INVALID_FIELD_TYPE", "Amount must be positive.");
        }
        return normalize(amount, currency);
    }

    public static String currency(String rawCurrency) {
        if (!StringUtils.hasText(rawCurrency)) {
            throw new MoneyValidationException("MISSING_REQUIRED_FIELD", "Currency is required.");
        }
        String normalized = rawCurrency.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}") || !FRACTION_DIGITS.containsKey(normalized)) {
            throw new MoneyValidationException("INVALID_CURRENCY", "Currency is not supported.");
        }
        return normalized;
    }

    private static BigDecimal normalize(BigDecimal amount, String currency) {
        String normalizedCurrency = currency(currency);
        int scale = FRACTION_DIGITS.get(normalizedCurrency);
        if (amount.stripTrailingZeros().scale() > scale) {
            throw new MoneyValidationException(
                    "INVALID_CURRENCY_PRECISION",
                    normalizedCurrency + " supports " + scale + " fractional digits."
            );
        }
        return amount.setScale(scale, RoundingMode.UNNECESSARY);
    }

    private static BigDecimal decimal(String rawAmount) {
        if (!StringUtils.hasText(rawAmount)) {
            throw new MoneyValidationException("MISSING_REQUIRED_FIELD", "Amount is required.");
        }
        try {
            return new BigDecimal(rawAmount.trim());
        } catch (NumberFormatException exception) {
            throw new MoneyValidationException("INVALID_FIELD_TYPE", "Amount must be a decimal number.");
        }
    }
}
