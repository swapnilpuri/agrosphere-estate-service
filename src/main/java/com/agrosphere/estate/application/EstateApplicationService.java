package com.agrosphere.estate.application;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.agrosphere.estate.domain.*;
import com.agrosphere.estate.application.port.out.*;
import com.agrosphere.estate.application.Requests.*;
@Service @Transactional
public class EstateApplicationService {
    private final EstateStore store;
    private final EstateAccess access;
    private final EventOutbox outbox;
    public EstateApplicationService(EstateStore store, EstateAccess access, EventOutbox outbox) { this.store=store; this.access=access; this.outbox=outbox; }
    private void version(Long supplied, Long current) { if (supplied == null || !supplied.equals(current)) throw DomainException.conflict("A current version is required"); }
    private void page(int page, int size) { if(page < 0 || size < 1 || size > 100 || (long)page*size > Integer.MAX_VALUE) throw DomainException.invalid("page must be nonnegative and size between 1 and 100"); }
    private Estate estate(UUID id) { return store.findEstate(id).orElseThrow(()->DomainException.missing("Estate")); }
    private Division division(UUID id) { return store.findDivision(id).orElseThrow(()->DomainException.missing("Division")); }
    private PlantationBlock block(UUID id) { return store.findPlantationBlock(id).orElseThrow(()->DomainException.missing("Block")); }
    public Estate createEstate(EstateWrite r) {
        access.platformAdmin(); if(r.version()!=null) throw DomainException.invalid("version must be absent on create");
        var now=Instant.now(); var e=store.saveEstate(new Estate(UUID.randomUUID(),r.estateCode(),r.name(),r.countryCode(),r.timezone(),r.totalAreaHectares(),r.status(),now,now,null));
        outbox.append("EstateCreated",e.id(),e.id(),e); return e;
    }
    @Transactional(readOnly=true) public Estate getEstate(UUID id) { access.read(id); return estate(id); }
    @Transactional(readOnly=true) public PageResult<Estate> estates(Status status,int page,int size) { page(page,size); return store.listEstate(null,status,access.readableEstates(),page,size); }
    public Estate updateEstate(UUID id,EstateWrite r) {
        access.write(id); var old=estate(id); version(r.version(),old.version());
        var e=store.saveEstate(new Estate(id,r.estateCode(),r.name(),r.countryCode(),r.timezone(),r.totalAreaHectares(),r.status(),old.createdAt(),old.updatedAt(),r.version()));
        outbox.append("EstateUpdated",id,id,e); return e;
    }
    public Estate estateStatus(UUID id, StatusWrite r) {
        access.write(id); var e=estate(id); version(r.version(),e.version());
        return updateEstate(id,new EstateWrite(e.estateCode(),e.name(),e.countryCode(),e.timezone(),e.totalAreaHectares(),r.status(),r.version()));
    }
    public Division createDivision(UUID estateId,DivisionWrite r) {
        access.write(estateId); estate(estateId); if(!estateId.equals(r.estateId())) throw DomainException.invalid("Division estate mismatch");
        if(r.version()!=null) throw DomainException.invalid("version must be absent on create"); var now=Instant.now();
        return store.saveDivision(new Division(UUID.randomUUID(),estateId,r.divisionCode(),r.name(),r.status(),now,now,null));
    }
    @Transactional(readOnly=true) public Division getDivision(UUID id) { var d=division(id); access.read(d.estateId()); return d; }
    @Transactional(readOnly=true) public PageResult<Division> divisions(UUID estateId,Status status,int page,int size) { access.read(estateId); estate(estateId); page(page,size); return store.listDivision(estateId,status,access.readableEstates(),page,size); }
    public Division updateDivision(UUID id,DivisionWrite r) {
        var d=division(id); access.write(d.estateId()); version(r.version(),d.version());
        if(!d.estateId().equals(r.estateId())) throw DomainException.invalid("Division cannot move between estates");
        return store.saveDivision(new Division(id,d.estateId(),r.divisionCode(),r.name(),r.status(),d.createdAt(),d.updatedAt(),r.version()));
    }
    private void blockRelationships(UUID estateId,UUID divisionId,String clone) {
        estate(estateId); if(!division(divisionId).estateId().equals(estateId)) throw DomainException.invalid("Block and division estate mismatch");
        if(clone!=null && !store.cloneExists(Rules.code(clone))) throw DomainException.invalid("Rubber clone does not exist");
    }
    public PlantationBlock createBlock(UUID divisionId,BlockWrite r) {
        var d=division(divisionId); access.write(d.estateId());
        if(!divisionId.equals(r.divisionId()) || !d.estateId().equals(r.estateId())) throw DomainException.invalid("Block parent mismatch");
        if(r.version()!=null) throw DomainException.invalid("version must be absent on create"); blockRelationships(r.estateId(),divisionId,r.rubberCloneCode());
        var now=Instant.now(); var b=store.savePlantationBlock(new PlantationBlock(UUID.randomUUID(),r.estateId(),divisionId,r.blockCode(),r.name(),r.cropType(),r.rubberCloneCode(),r.plantingDate(),r.plantedAreaHectares(),r.plantedTreeCount(),r.tappableTreeCount(),r.status(),now,now,null));
        outbox.append("PlantationBlockCreated",b.estateId(),b.id(),b); return b;
    }
    @Transactional(readOnly=true) public PlantationBlock getBlock(UUID id) { var b=block(id); access.read(b.estateId()); return b; }
    @Transactional(readOnly=true) public PageResult<PlantationBlock> blocks(UUID estateId,Status status,int page,int size) { access.read(estateId); estate(estateId); page(page,size); return store.listPlantationBlock(estateId,status,access.readableEstates(),page,size); }
    public PlantationBlock updateBlock(UUID id,BlockWrite r) {
        var b=block(id); access.write(b.estateId()); version(r.version(),b.version());
        if(!b.estateId().equals(r.estateId())) throw DomainException.invalid("Block cannot move between estates");
        blockRelationships(b.estateId(),r.divisionId(),r.rubberCloneCode());
        var saved=store.savePlantationBlock(new PlantationBlock(id,b.estateId(),r.divisionId(),r.blockCode(),r.name(),r.cropType(),r.rubberCloneCode(),r.plantingDate(),r.plantedAreaHectares(),r.plantedTreeCount(),r.tappableTreeCount(),r.status(),b.createdAt(),b.updatedAt(),r.version()));
        outbox.append("PlantationBlockUpdated",saved.estateId(),id,saved); return saved;
    }
    public PlantationBlock blockStatus(UUID id,StatusWrite r) {
        var b=block(id); access.write(b.estateId()); version(r.version(),b.version());
        return updateBlock(id,new BlockWrite(b.estateId(),b.divisionId(),b.blockCode(),b.name(),b.cropType(),b.rubberCloneCode(),b.plantingDate(),b.plantedAreaHectares(),b.plantedTreeCount(),b.tappableTreeCount(),r.status(),r.version()));
    }
    public RubberClone createClone(CloneWrite r) { access.platformAdmin(); return store.saveRubberClone(new RubberClone(UUID.randomUUID(),r.cloneCode(),r.description(),r.status())); }
    @Transactional(readOnly=true) public PageResult<RubberClone> clones(Status status,int page,int size) { access.cloneRead(); page(page,size); return store.listRubberClone(null,status,null,page,size); }
}
