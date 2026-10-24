package com.agrosphere.estate.adapter.out.persistence;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.persistence.*;
import com.agrosphere.estate.domain.*;
@Entity @Table(name="plantation_block")
public class PlantationBlockEntity {
    @Id @Column(name="id")
    public UUID id;
    @Column(name="estate_id")
    public UUID estateId;
    @Column(name="division_id")
    public UUID divisionId;
    @Column(name="block_code")
    public String blockCode;
    @Column(name="name")
    public String name;
    @Enumerated(EnumType.STRING) @Column(name="crop_type")
    public CropType cropType;
    @Column(name="rubber_clone_code")
    public String rubberCloneCode;
    @Column(name="planting_date")
    public LocalDate plantingDate;
    @Column(name="planted_area_hectares", precision=14, scale=4)
    public BigDecimal plantedAreaHectares;
    @Column(name="planted_tree_count")
    public Integer plantedTreeCount;
    @Column(name="tappable_tree_count")
    public Integer tappableTreeCount;
    @Enumerated(EnumType.STRING) @Column(name="status")
    public Status status;
    @Column(name="created_at")
    public Instant createdAt;
    @Column(name="updated_at")
    public Instant updatedAt;
    @Version @Column(name="version")
    public Long version;
    protected PlantationBlockEntity() {}
    public static PlantationBlockEntity from(PlantationBlock value) {
        var entity = new PlantationBlockEntity();
        entity.id = value.id();
        entity.estateId = value.estateId();
        entity.divisionId = value.divisionId();
        entity.blockCode = value.blockCode();
        entity.name = value.name();
        entity.cropType = value.cropType();
        entity.rubberCloneCode = value.rubberCloneCode();
        entity.plantingDate = value.plantingDate();
        entity.plantedAreaHectares = value.plantedAreaHectares();
        entity.plantedTreeCount = value.plantedTreeCount();
        entity.tappableTreeCount = value.tappableTreeCount();
        entity.status = value.status();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        entity.version = value.version();
        return entity;
    }
    public PlantationBlock toDomain() { return new PlantationBlock(id, estateId, divisionId, blockCode, name, cropType, rubberCloneCode, plantingDate, plantedAreaHectares, plantedTreeCount, tappableTreeCount, status, createdAt, updatedAt, version); }
    public void copy(PlantationBlock value) {
        this.estateId = value.estateId();
        this.divisionId = value.divisionId();
        this.blockCode = value.blockCode();
        this.name = value.name();
        this.cropType = value.cropType();
        this.rubberCloneCode = value.rubberCloneCode();
        this.plantingDate = value.plantingDate();
        this.plantedAreaHectares = value.plantedAreaHectares();
        this.plantedTreeCount = value.plantedTreeCount();
        this.tappableTreeCount = value.tappableTreeCount();
        this.status = value.status();
        this.updatedAt = Instant.now();
    }
}
