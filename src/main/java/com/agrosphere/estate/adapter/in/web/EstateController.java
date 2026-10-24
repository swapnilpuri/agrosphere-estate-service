package com.agrosphere.estate.adapter.in.web;
import java.net.URI;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.agrosphere.estate.application.*;
import com.agrosphere.estate.application.Requests.*;
import com.agrosphere.estate.application.Responses.*;
import com.agrosphere.estate.domain.Status;
@RestController @RequestMapping("/api/v1") @SecurityRequirement(name="bearerAuth")
public class EstateController {
    private final EstateApplicationService service;
    public EstateController(EstateApplicationService service) { this.service=service; }
    @PostMapping("/estates") public ResponseEntity<EstateView> createEstate(@Valid @RequestBody EstateWrite request) {
        var view=EstateView.from(service.createEstate(request)); return ResponseEntity.created(URI.create("/api/v1/estates/"+view.id())).body(view);
    }
    @GetMapping("/estates") public PageResult<EstateView> estates(@RequestParam(required=false) Status status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return Responses.map(service.estates(status,page,size),EstateView::from); }
    @GetMapping("/estates/{id}") public EstateView estate(@PathVariable UUID id) { return EstateView.from(service.getEstate(id)); }
    @PutMapping("/estates/{id}") public EstateView updateEstate(@PathVariable UUID id,@Valid @RequestBody EstateWrite request) { return EstateView.from(service.updateEstate(id,request)); }
    @PatchMapping("/estates/{id}/status") public EstateView estateStatus(@PathVariable UUID id,@Valid @RequestBody StatusWrite request) { return EstateView.from(service.estateStatus(id,request)); }
    @PostMapping("/estates/{estateId}/divisions") public ResponseEntity<DivisionView> createDivision(@PathVariable UUID estateId,@Valid @RequestBody DivisionWrite request) {
        var view=DivisionView.from(service.createDivision(estateId,request)); return ResponseEntity.created(URI.create("/api/v1/divisions/"+view.id())).body(view);
    }
    @GetMapping("/estates/{estateId}/divisions") public PageResult<DivisionView> divisions(@PathVariable UUID estateId,@RequestParam(required=false) Status status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return Responses.map(service.divisions(estateId,status,page,size),DivisionView::from); }
    @GetMapping("/divisions/{id}") public DivisionView division(@PathVariable UUID id) { return DivisionView.from(service.getDivision(id)); }
    @PutMapping("/divisions/{id}") public DivisionView updateDivision(@PathVariable UUID id,@Valid @RequestBody DivisionWrite request) { return DivisionView.from(service.updateDivision(id,request)); }
    @PostMapping("/divisions/{divisionId}/blocks") public ResponseEntity<PlantationBlockView> createBlock(@PathVariable UUID divisionId,@Valid @RequestBody BlockWrite request) {
        var view=PlantationBlockView.from(service.createBlock(divisionId,request)); return ResponseEntity.created(URI.create("/api/v1/blocks/"+view.id())).body(view);
    }
    @GetMapping("/estates/{estateId}/blocks") public PageResult<PlantationBlockView> blocks(@PathVariable UUID estateId,@RequestParam(required=false) Status status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return Responses.map(service.blocks(estateId,status,page,size),PlantationBlockView::from); }
    @GetMapping("/blocks/{id}") public PlantationBlockView block(@PathVariable UUID id) { return PlantationBlockView.from(service.getBlock(id)); }
    @PutMapping("/blocks/{id}") public PlantationBlockView updateBlock(@PathVariable UUID id,@Valid @RequestBody BlockWrite request) { return PlantationBlockView.from(service.updateBlock(id,request)); }
    @PatchMapping("/blocks/{id}/status") public PlantationBlockView blockStatus(@PathVariable UUID id,@Valid @RequestBody StatusWrite request) { return PlantationBlockView.from(service.blockStatus(id,request)); }
    @GetMapping("/rubber-clones") public PageResult<RubberCloneView> clones(@RequestParam(required=false) Status status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return Responses.map(service.clones(status,page,size),RubberCloneView::from); }
    @PostMapping("/rubber-clones") public ResponseEntity<RubberCloneView> createClone(@Valid @RequestBody CloneWrite request) {
        var view=RubberCloneView.from(service.createClone(request)); return ResponseEntity.status(201).body(view);
    }
}
