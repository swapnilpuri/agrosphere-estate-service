package com.agrosphere.estate.domain;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

public record RubberClone(UUID id, String cloneCode, String description, Status status) {
    public RubberClone {
        Rules.required(id, "id"); Rules.required(status, "status");
        cloneCode = Rules.code(cloneCode); description = Rules.text(description, "description", 1000);
    }
}
