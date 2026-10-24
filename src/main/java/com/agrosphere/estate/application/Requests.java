package com.agrosphere.estate.application;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import com.agrosphere.estate.domain.*;
public final class Requests {
    private Requests() {}
    public record EstateWrite(@NotBlank @Size(max=64) String estateCode, @NotBlank @Size(max=200) String name,
        @NotBlank @Size(min=2,max=2) String countryCode, @NotBlank @Size(max=100) String timezone,
        @Digits(integer=10,fraction=4) @PositiveOrZero BigDecimal totalAreaHectares, @NotNull Status status, @PositiveOrZero Long version) {}
    public record DivisionWrite(@NotNull UUID estateId, @NotBlank @Size(max=64) String divisionCode,
        @NotBlank @Size(max=200) String name, @NotNull Status status, @PositiveOrZero Long version) {}
    public record BlockWrite(@NotNull UUID estateId, @NotNull UUID divisionId, @NotBlank @Size(max=64) String blockCode,
        @NotBlank @Size(max=200) String name, @NotNull CropType cropType, @Size(max=64) String rubberCloneCode,
        LocalDate plantingDate, @NotNull @Digits(integer=10,fraction=4) @PositiveOrZero BigDecimal plantedAreaHectares,
        @NotNull @PositiveOrZero Integer plantedTreeCount, @NotNull @PositiveOrZero Integer tappableTreeCount,
        @NotNull Status status, @PositiveOrZero Long version) {}
    public record CloneWrite(@NotBlank @Size(max=64) String cloneCode, @NotBlank @Size(max=1000) String description, @NotNull Status status) {}
    public record StatusWrite(@NotNull Status status, @NotNull @PositiveOrZero Long version) {}
}
