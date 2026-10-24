package com.agrosphere.estate;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.agrosphere.estate.domain.*;
class DomainRulesTest {
    static final UUID ESTATE=UUID.fromString("00000000-0000-0000-0000-000000000001"), DIVISION=UUID.fromString("00000000-0000-0000-0000-000000000002");
    PlantationBlock block(String area,int planted,int tappable,Status status) { return new PlantationBlock(DIVISION,ESTATE,DIVISION,"b-01","Block",CropType.RUBBER,null,LocalDate.of(2020,1,1),new BigDecimal(area),planted,tappable,status,Instant.EPOCH,Instant.EPOCH,0L); }
    @Test void codesAreNormalized() { assertEquals("B-01",block("1",20,10,Status.ACTIVE).blockCode()); }
    @Test void activeAreaMustBePositive() { assertThrows(DomainException.class,()->block("0",20,10,Status.ACTIVE)); }
    @Test void inactiveCanHaveZeroArea() { assertFalse(block("0",20,10,Status.INACTIVE).eligibleForOperations()); }
    @Test void negativeAreaIsRejectedEvenWhenInactive() { assertThrows(DomainException.class,()->block("-1",20,10,Status.INACTIVE)); }
    @Test void negativeCountsAreRejected() { assertThrows(DomainException.class,()->block("1",-1,0,Status.ACTIVE)); assertThrows(DomainException.class,()->block("1",1,-1,Status.ACTIVE)); }
    @Test void tappableCannotExceedPlanted() { assertThrows(DomainException.class,()->block("1",1,2,Status.ACTIVE)); }
    @Test void eligibilityRequiresActiveAndTappableTrees() { assertTrue(block("1",20,10,Status.ACTIVE).eligibleForOperations()); assertFalse(block("1",20,0,Status.ACTIVE).eligibleForOperations()); }
    @Test void invalidTimezoneAndCountryAreRejected() {
        assertThrows(DomainException.class,()->new Estate(ESTATE,"E1","Estate","ZZ","Asia/Colombo",null,Status.ACTIVE,Instant.EPOCH,Instant.EPOCH,0L));
        assertThrows(DomainException.class,()->new Estate(ESTATE,"E1","Estate","LK","bad-zone",null,Status.ACTIVE,Instant.EPOCH,Instant.EPOCH,0L));
    }
    @Test void precisionAndMissingParentsAreRejected() { assertThrows(DomainException.class,()->block("1.00001",2,1,Status.ACTIVE)); assertThrows(DomainException.class,()->new Division(DIVISION,null,"D1","Division",Status.ACTIVE,Instant.EPOCH,Instant.EPOCH,0L)); }
    @Test void cloneCodeAndDescriptionAreRequired() { assertThrows(DomainException.class,()->new RubberClone(ESTATE,"invalid code","Description",Status.ACTIVE)); assertThrows(DomainException.class,()->new RubberClone(ESTATE,"RRIC100"," ",Status.ACTIVE)); }
}
