package com.agrosphere.estate.application;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import com.agrosphere.estate.domain.*;
public final class Responses {
    private Responses() {}
    public record EstateView(UUID id, String estateCode, String name, String countryCode, String timezone, BigDecimal totalAreaHectares, Status status, Instant createdAt, Instant updatedAt, Long version) {
        public static EstateView from(Estate value) { return new EstateView(value.id(), value.estateCode(), value.name(), value.countryCode(), value.timezone(), value.totalAreaHectares(), value.status(), value.createdAt(), value.updatedAt(), value.version()); }
    }
    public record DivisionView(UUID id, UUID estateId, String divisionCode, String name, Status status, Instant createdAt, Instant updatedAt, Long version) {
        public static DivisionView from(Division value) { return new DivisionView(value.id(), value.estateId(), value.divisionCode(), value.name(), value.status(), value.createdAt(), value.updatedAt(), value.version()); }
    }
    public record PlantationBlockView(UUID id, UUID estateId, UUID divisionId, String blockCode, String name, CropType cropType, String rubberCloneCode, LocalDate plantingDate, BigDecimal plantedAreaHectares, Integer plantedTreeCount, Integer tappableTreeCount, Status status, Instant createdAt, Instant updatedAt, Long version) {
        public static PlantationBlockView from(PlantationBlock value) { return new PlantationBlockView(value.id(), value.estateId(), value.divisionId(), value.blockCode(), value.name(), value.cropType(), value.rubberCloneCode(), value.plantingDate(), value.plantedAreaHectares(), value.plantedTreeCount(), value.tappableTreeCount(), value.status(), value.createdAt(), value.updatedAt(), value.version()); }
    }
    public record RubberCloneView(UUID id, String cloneCode, String description, Status status) {
        public static RubberCloneView from(RubberClone value) { return new RubberCloneView(value.id(), value.cloneCode(), value.description(), value.status()); }
    }
    public static <T,R> PageResult<R> map(PageResult<T> page, java.util.function.Function<T,R> mapper) { return new PageResult<>(page.content().stream().map(mapper).toList(),page.page(),page.size(),page.totalElements()); }
}
