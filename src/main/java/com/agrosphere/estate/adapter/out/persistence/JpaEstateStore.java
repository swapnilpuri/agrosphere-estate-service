package com.agrosphere.estate.adapter.out.persistence;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;
import com.agrosphere.estate.domain.*;
import com.agrosphere.estate.application.*;
import com.agrosphere.estate.application.port.out.EstateStore;
@Repository
public class JpaEstateStore implements EstateStore {
    private final EntityManager em;
    public JpaEstateStore(EntityManager em) { this.em = em; }
    @Override public Optional<Estate> findEstate(UUID id) { return Optional.ofNullable(em.find(EstateEntity.class,id)).map(EstateEntity::toDomain); }
    @Override public Estate saveEstate(Estate value) {
        var entity = em.find(EstateEntity.class,value.id());
        if (entity == null) { entity = EstateEntity.from(value); em.persist(entity); }
        else {
            if (!Objects.equals(entity.version,value.version())) throw DomainException.conflict("Stale version");
            entity.copy(value);
        }
        em.flush();
        return entity.toDomain();
    }
    @Override public PageResult<Estate> listEstate(UUID estateId, Status status, Set<UUID> allowed, int page, int size) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(EstateEntity.class); var row = query.from(EstateEntity.class);
        query.select(row).where(filters(cb,row,estateId,status,allowed,"id")).orderBy(cb.asc(row.get("id")));
        var count = cb.createQuery(Long.class); var counted = count.from(EstateEntity.class);
        count.select(cb.count(counted)).where(filters(cb,counted,estateId,status,allowed,"id"));
        return new PageResult<>(em.createQuery(query).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList().stream().map(EstateEntity::toDomain).toList(),page,size,em.createQuery(count).getSingleResult());
    }

    @Override public Optional<Division> findDivision(UUID id) { return Optional.ofNullable(em.find(DivisionEntity.class,id)).map(DivisionEntity::toDomain); }
    @Override public Division saveDivision(Division value) {
        var entity = em.find(DivisionEntity.class,value.id());
        if (entity == null) { entity = DivisionEntity.from(value); em.persist(entity); }
        else {
            if (!Objects.equals(entity.version,value.version())) throw DomainException.conflict("Stale version");
            entity.copy(value);
        }
        em.flush();
        return entity.toDomain();
    }
    @Override public PageResult<Division> listDivision(UUID estateId, Status status, Set<UUID> allowed, int page, int size) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(DivisionEntity.class); var row = query.from(DivisionEntity.class);
        query.select(row).where(filters(cb,row,estateId,status,allowed,"estateId")).orderBy(cb.asc(row.get("id")));
        var count = cb.createQuery(Long.class); var counted = count.from(DivisionEntity.class);
        count.select(cb.count(counted)).where(filters(cb,counted,estateId,status,allowed,"estateId"));
        return new PageResult<>(em.createQuery(query).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList().stream().map(DivisionEntity::toDomain).toList(),page,size,em.createQuery(count).getSingleResult());
    }

    @Override public Optional<PlantationBlock> findPlantationBlock(UUID id) { return Optional.ofNullable(em.find(PlantationBlockEntity.class,id)).map(PlantationBlockEntity::toDomain); }
    @Override public PlantationBlock savePlantationBlock(PlantationBlock value) {
        var entity = em.find(PlantationBlockEntity.class,value.id());
        if (entity == null) { entity = PlantationBlockEntity.from(value); em.persist(entity); }
        else {
            if (!Objects.equals(entity.version,value.version())) throw DomainException.conflict("Stale version");
            entity.copy(value);
        }
        em.flush();
        return entity.toDomain();
    }
    @Override public PageResult<PlantationBlock> listPlantationBlock(UUID estateId, Status status, Set<UUID> allowed, int page, int size) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(PlantationBlockEntity.class); var row = query.from(PlantationBlockEntity.class);
        query.select(row).where(filters(cb,row,estateId,status,allowed,"estateId")).orderBy(cb.asc(row.get("id")));
        var count = cb.createQuery(Long.class); var counted = count.from(PlantationBlockEntity.class);
        count.select(cb.count(counted)).where(filters(cb,counted,estateId,status,allowed,"estateId"));
        return new PageResult<>(em.createQuery(query).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList().stream().map(PlantationBlockEntity::toDomain).toList(),page,size,em.createQuery(count).getSingleResult());
    }

    @Override public Optional<RubberClone> findRubberClone(UUID id) { return Optional.ofNullable(em.find(RubberCloneEntity.class,id)).map(RubberCloneEntity::toDomain); }
    @Override public RubberClone saveRubberClone(RubberClone value) {
        var entity = em.find(RubberCloneEntity.class,value.id());
        if (entity == null) { entity = RubberCloneEntity.from(value); em.persist(entity); }
        else {

            entity.copy(value);
        }
        em.flush();
        return entity.toDomain();
    }
    @Override public PageResult<RubberClone> listRubberClone(UUID estateId, Status status, Set<UUID> allowed, int page, int size) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(RubberCloneEntity.class); var row = query.from(RubberCloneEntity.class);
        query.select(row).where(filters(cb,row,estateId,status,allowed,"")).orderBy(cb.asc(row.get("id")));
        var count = cb.createQuery(Long.class); var counted = count.from(RubberCloneEntity.class);
        count.select(cb.count(counted)).where(filters(cb,counted,estateId,status,allowed,""));
        return new PageResult<>(em.createQuery(query).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList().stream().map(RubberCloneEntity::toDomain).toList(),page,size,em.createQuery(count).getSingleResult());
    }

    private Predicate[] filters(jakarta.persistence.criteria.CriteriaBuilder cb, Root<?> row, UUID estateId, Status status, Set<UUID> allowed, String estateField) {
        var filters = new ArrayList<Predicate>();
        if (status != null) filters.add(cb.equal(row.get("status"),status));
        if (!estateField.isEmpty()) {
            if (estateId != null) filters.add(cb.equal(row.get(estateField),estateId));
            if (allowed != null) filters.add(allowed.isEmpty() ? cb.disjunction() : row.get(estateField).in(allowed));
        }
        return filters.toArray(Predicate[]::new);
    }
    @Override public boolean cloneExists(String code) { return em.createQuery("select count(c) from RubberCloneEntity c where c.cloneCode = :code",Long.class).setParameter("code",code).getSingleResult() > 0; }
}
