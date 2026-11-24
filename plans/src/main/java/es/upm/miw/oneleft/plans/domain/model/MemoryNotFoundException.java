package es.upm.miw.oneleft.plans.domain.model;

import java.util.UUID;

public class MemoryNotFoundException extends RuntimeException {

    public static final String CODE = "memory.notFound";

    public MemoryNotFoundException(UUID memoryId) {
        super("There is no photo " + memoryId + " in the album of this plan");
    }
}
