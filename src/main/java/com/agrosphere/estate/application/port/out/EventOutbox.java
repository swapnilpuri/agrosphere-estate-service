package com.agrosphere.estate.application.port.out;
import java.util.UUID;
public interface EventOutbox { void append(String eventType, UUID estateId, UUID entityId, Object payload); }
