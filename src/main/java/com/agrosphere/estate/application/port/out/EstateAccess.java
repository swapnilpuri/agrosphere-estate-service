package com.agrosphere.estate.application.port.out;
import java.util.*;
public interface EstateAccess {
    void read(UUID estateId);
    void write(UUID estateId);
    void platformAdmin();
    void cloneRead();
    Set<UUID> readableEstates(); // null means validated PLATFORM_ADMIN
}
