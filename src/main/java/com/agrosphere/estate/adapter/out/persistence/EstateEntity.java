package com.agrosphere.estate.adapter.out.persistence;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.persistence.*;
import com.agrosphere.estate.domain.*;
@Entity @Table(name="estate")
public class EstateEntity {
    @Id @Column(name="id")
    public UUID id;
    @Column(name="estate_code")
    public String estateCode;
    @Column(name="name")
    public String name;
    @Column(name="country_code")
    public String countryCode;
    @Column(name="timezone")
    public String timezone;
    @Column(name="total_area_hectares", precision=14, scale=4)
    public BigDecimal totalAreaHectares;
    @Enumerated(EnumType.STRING) @Column(name="status")
    public Status status;
    @Column(name="created_at")
    public Instant createdAt;
    @Column(name="updated_at")
    public Instant updatedAt;
    @Version @Column(name="version")
    public Long version;
    protected EstateEntity() {}
    public static EstateEntity from(Estate value) {
        var entity = new EstateEntity();
        entity.id = value.id();
        entity.estateCode = value.estateCode();
        entity.name = value.name();
        entity.countryCode = value.countryCode();
        entity.timezone = value.timezone();
        entity.totalAreaHectares = value.totalAreaHectares();
        entity.status = value.status();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        entity.version = value.version();
        return entity;
    }
    public Estate toDomain() { return new Estate(id, estateCode, name, countryCode, timezone, totalAreaHectares, status, createdAt, updatedAt, version); }
    public void copy(Estate value) {
        this.estateCode = value.estateCode();
        this.name = value.name();
        this.countryCode = value.countryCode();
        this.timezone = value.timezone();
        this.totalAreaHectares = value.totalAreaHectares();
        this.status = value.status();
        this.updatedAt = Instant.now();
    }
}
