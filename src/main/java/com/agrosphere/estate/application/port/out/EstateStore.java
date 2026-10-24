package com.agrosphere.estate.application.port.out;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import com.agrosphere.estate.domain.*;
import com.agrosphere.estate.application.PageResult;
public interface EstateStore {
    Optional<Estate> findEstate(UUID id);
    Estate saveEstate(Estate value);
    PageResult<Estate> listEstate(UUID estateId, Status status, Set<UUID> allowedEstates, int page, int size);
    Optional<Division> findDivision(UUID id);
    Division saveDivision(Division value);
    PageResult<Division> listDivision(UUID estateId, Status status, Set<UUID> allowedEstates, int page, int size);
    Optional<PlantationBlock> findPlantationBlock(UUID id);
    PlantationBlock savePlantationBlock(PlantationBlock value);
    PageResult<PlantationBlock> listPlantationBlock(UUID estateId, Status status, Set<UUID> allowedEstates, int page, int size);
    Optional<RubberClone> findRubberClone(UUID id);
    RubberClone saveRubberClone(RubberClone value);
    PageResult<RubberClone> listRubberClone(UUID estateId, Status status, Set<UUID> allowedEstates, int page, int size);
    boolean cloneExists(String code);
}
