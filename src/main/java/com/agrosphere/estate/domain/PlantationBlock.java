package com.agrosphere.estate.domain;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

public record PlantationBlock(UUID id, UUID estateId, UUID divisionId, String blockCode, String name, CropType cropType, String rubberCloneCode, LocalDate plantingDate, BigDecimal plantedAreaHectares, Integer plantedTreeCount, Integer tappableTreeCount, Status status, Instant createdAt, Instant updatedAt, Long version) {
    public PlantationBlock {
        Rules.required(id, "id"); Rules.required(status, "status");
        Rules.required(estateId, "estateId"); Rules.required(divisionId, "divisionId");
        blockCode = Rules.code(blockCode); name = Rules.text(name, "name", 200); Rules.required(cropType, "cropType");
        if (rubberCloneCode != null) rubberCloneCode = Rules.code(rubberCloneCode);
        Rules.area(plantedAreaHectares, status == Status.ACTIVE);
        if (plantedTreeCount == null || tappableTreeCount == null || plantedTreeCount < 0 || tappableTreeCount < 0 || tappableTreeCount > plantedTreeCount)
            throw DomainException.invalid("Tree counts must be nonnegative and tappable count cannot exceed planted count");
    }
    public boolean eligibleForOperations() { return status == Status.ACTIVE && tappableTreeCount > 0; }
}
