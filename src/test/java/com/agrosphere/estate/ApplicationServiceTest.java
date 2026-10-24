package com.agrosphere.estate;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.agrosphere.estate.domain.*;
import com.agrosphere.estate.application.*;
import com.agrosphere.estate.application.Requests.*;
import com.agrosphere.estate.application.port.out.*;
class ApplicationServiceTest {
    EstateStore store; EstateAccess access; EventOutbox outbox; EstateApplicationService service;
    UUID id=DomainRulesTest.ESTATE; UUID division=DomainRulesTest.DIVISION;
    Estate original=new Estate(id,"E1","Estate","LK","Asia/Colombo",null,Status.ACTIVE,Instant.EPOCH,Instant.EPOCH,4L);
    @BeforeEach void setup() { store=mock(EstateStore.class);access=mock(EstateAccess.class);outbox=mock(EventOutbox.class);service=new EstateApplicationService(store,access,outbox);when(store.findEstate(id)).thenReturn(Optional.of(original)); }
    @Test void updatePreservesIdentityAndCreationTimeAndWritesOutbox() {
        when(store.saveEstate(any())).thenAnswer(i->i.getArgument(0));
        var saved=service.updateEstate(id,new EstateWrite("E1","Updated","LK","Asia/Colombo",null,Status.ACTIVE,4L));
        assertEquals(id,saved.id());assertEquals(Instant.EPOCH,saved.createdAt());verify(outbox).append("EstateUpdated",id,id,saved);verify(access).write(id);
    }
    @Test void staleUpdateCannotWrite() { assertThrows(DomainException.class,()->service.updateEstate(id,new EstateWrite("E1","Updated","LK","Asia/Colombo",null,Status.ACTIVE,3L)));verify(store,never()).saveEstate(any());verifyNoInteractions(outbox); }
    @Test void statusTransitionProducesUpdateEvent() { when(store.saveEstate(any())).thenAnswer(i->i.getArgument(0));var e=service.estateStatus(id,new StatusWrite(Status.INACTIVE,4L));assertEquals(Status.INACTIVE,e.status());verify(outbox).append("EstateUpdated",id,id,e); }
    @Test void duplicateConflictStopsEventCreation() { when(store.saveEstate(any())).thenThrow(DomainException.conflict("Duplicate code"));assertThrows(DomainException.class,()->service.createEstate(new EstateWrite("E1","New","LK","Asia/Colombo",null,Status.ACTIVE,null)));verifyNoInteractions(outbox); }
    @Test void divisionEstateMismatchIsRejected() { assertThrows(DomainException.class,()->service.createDivision(id,new DivisionWrite(division,"D1","Division",Status.ACTIVE,null)));verify(store,never()).saveDivision(any()); }
    @Test void divisionCannotMoveEstates() { when(store.findDivision(division)).thenReturn(Optional.of(new Division(division,id,"D1","Division",Status.ACTIVE,Instant.EPOCH,Instant.EPOCH,0L)));assertThrows(DomainException.class,()->service.updateDivision(division,new DivisionWrite(division,"D1","Division",Status.ACTIVE,0L))); }
    @Test void invalidPaginationIsRejected() { assertThrows(DomainException.class,()->service.estates(null,0,101));assertThrows(DomainException.class,()->service.estates(null,Integer.MAX_VALUE,100)); }
    @Test void missingEstateReturnsDomainNotFound() { when(store.findEstate(id)).thenReturn(Optional.empty());assertEquals(DomainException.Kind.NOT_FOUND,assertThrows(DomainException.class,()->service.getEstate(id)).kind()); }
    @Test void authorizationPrecedesEstateMutation() { doThrow(new org.springframework.security.access.AccessDeniedException("denied")).when(access).write(id);assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.estateStatus(id,new StatusWrite(Status.INACTIVE,4L)));verify(store,never()).saveEstate(any()); }
}
