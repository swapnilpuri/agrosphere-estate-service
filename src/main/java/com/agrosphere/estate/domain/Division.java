package com.agrosphere.estate.domain;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

public record Division(UUID id, UUID estateId, String divisionCode, String name, Status status, Instant createdAt, Instant updatedAt, Long version) {
    public Division {
        Rules.required(id, "id"); Rules.required(status, "status");
        Rules.required(estateId, "estateId"); divisionCode = Rules.code(divisionCode); name = Rules.text(name, "name", 200);
    }
}
