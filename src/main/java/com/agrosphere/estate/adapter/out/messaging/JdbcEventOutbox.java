package com.agrosphere.estate.adapter.out.messaging;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import com.agrosphere.estate.application.port.out.EventOutbox;
import com.agrosphere.estate.application.CorrelationContext;
@Component
public class JdbcEventOutbox implements EventOutbox {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final String provenance;
    public JdbcEventOutbox(JdbcTemplate jdbc,ObjectMapper json,@org.springframework.beans.factory.annotation.Value("${estate.data-provenance:REAL}") String provenance) {
        if(!Set.of("REAL","SIMULATED","ESTIMATED","PREDICTED").contains(provenance)) throw new IllegalArgumentException("Invalid data provenance");
        this.jdbc=jdbc; this.json=json; this.provenance=provenance;
    }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public void append(String type,UUID estateId,UUID entityId,Object payload) {
        UUID eventId=UUID.randomUUID(), correlationId=CorrelationContext.current(); Instant now=Instant.now();
        var event=new LinkedHashMap<String,Object>();
        event.put("eventId",eventId); event.put("eventType",type); event.put("eventVersion",2);
        event.put("occurredAt",now); event.put("correlationId",correlationId); event.put("sourceService","estate-service");
        event.put("estateId",estateId); event.put("entityId",entityId); event.put("dataProvenance",provenance);
        if(type.startsWith("PlantationBlock")) event.put("blockId",entityId);
        event.put("payload",payload);
        var transport=TraceTransport.capture();
        jdbc.update("INSERT INTO outbox_event(event_id,aggregate_id,event_type,event_version,correlation_id,occurred_at,payload,trace_parent,trace_state) VALUES (?,?,?,2,?,?,CAST(? AS jsonb),?,?)",eventId,entityId,type,correlationId,java.sql.Timestamp.from(now),json.writeValueAsString(event),transport.get("traceparent"),transport.get("tracestate"));
    }
}
