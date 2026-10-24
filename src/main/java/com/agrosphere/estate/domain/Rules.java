package com.agrosphere.estate.domain;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

public final class Rules {
    private Rules() {}
    public static String text(String value, String field, int max) {
        if (value == null || value.isBlank() || value.length() > max) throw DomainException.invalid(field + " is required and must be at most " + max + " characters");
        return value.trim();
    }
    public static String code(String value) {
        String code = text(value, "code", 64).toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9][A-Z0-9_-]*")) throw DomainException.invalid("Invalid code");
        return code;
    }
    public static void required(Object value, String field) { if (value == null) throw DomainException.invalid(field + " is required"); }
    public static void area(BigDecimal area, boolean positive) {
        if (area == null || area.signum() < 0 || (positive && area.signum() == 0) || area.scale() > 4 || area.precision() - area.scale() > 10)
            throw DomainException.invalid("Area must be " + (positive ? "positive" : "nonnegative") + " with at most 10 integer and 4 fractional digits");
    }
}
