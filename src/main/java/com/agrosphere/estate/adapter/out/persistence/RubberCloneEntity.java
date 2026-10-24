package com.agrosphere.estate.adapter.out.persistence;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.persistence.*;
import com.agrosphere.estate.domain.*;
@Entity @Table(name="rubber_clone")
public class RubberCloneEntity {
    @Id @Column(name="id")
    public UUID id;
    @Column(name="clone_code")
    public String cloneCode;
    @Column(name="description")
    public String description;
    @Enumerated(EnumType.STRING) @Column(name="status")
    public Status status;
    protected RubberCloneEntity() {}
    public static RubberCloneEntity from(RubberClone value) {
        var entity = new RubberCloneEntity();
        entity.id = value.id();
        entity.cloneCode = value.cloneCode();
        entity.description = value.description();
        entity.status = value.status();
        return entity;
    }
    public RubberClone toDomain() { return new RubberClone(id, cloneCode, description, status); }
    public void copy(RubberClone value) {
        this.cloneCode = value.cloneCode();
        this.description = value.description();
        this.status = value.status();

    }
}
