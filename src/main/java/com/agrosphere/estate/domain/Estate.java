package com.agrosphere.estate.domain;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

public record Estate(UUID id, String estateCode, String name, String countryCode, String timezone, BigDecimal totalAreaHectares, Status status, Instant createdAt, Instant updatedAt, Long version) {
    public Estate {
        Rules.required(id, "id"); Rules.required(status, "status");
        estateCode = Rules.code(estateCode); name = Rules.text(name, "name", 200);
        countryCode = Rules.text(countryCode, "countryCode", 2).toUpperCase(Locale.ROOT);
        if (!Set.of(Locale.getISOCountries()).contains(countryCode)) throw DomainException.invalid("countryCode must be ISO 3166-1 alpha-2");
        try { ZoneId.of(timezone); } catch (Exception ex) { throw DomainException.invalid("Invalid IANA timezone"); }
        if (totalAreaHectares != null) Rules.area(totalAreaHectares, false);
    }
}
