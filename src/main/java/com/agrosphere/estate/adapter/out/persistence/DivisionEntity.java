package com.agrosphere.estate.adapter.out.persistence;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.persistence.*;
import com.agrosphere.estate.domain.*;
@Entity @Table(name="division")
public class DivisionEntity {
    @Id @Column(name="id")
    public UUID id;
    @Column(name="estate_id")
    public UUID estateId;
    @Column(name="division_code")
    public String divisionCode;
    @Column(name="name")
    public String name;
    @Enumerated(EnumType.STRING) @Column(name="status")
    public Status status;
    @Column(name="created_at")
    public Instant createdAt;
    @Column(name="updated_at")
    public Instant updatedAt;
    @Version @Column(name="version")
    public Long version;
    protected DivisionEntity() {}
    public static DivisionEntity from(Division value) {
        var entity = new DivisionEntity();
        entity.id = value.id();
        entity.estateId = value.estateId();
        entity.divisionCode = value.divisionCode();
        entity.name = value.name();
        entity.status = value.status();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        entity.version = value.version();
        return entity;
    }
    public Division toDomain() { return new Division(id, estateId, divisionCode, name, status, createdAt, updatedAt, version); }
    public void copy(Division value) {
        this.estateId = value.estateId();
        this.divisionCode = value.divisionCode();
        this.name = value.name();
        this.status = value.status();
        this.updatedAt = Instant.now();
    }
}
