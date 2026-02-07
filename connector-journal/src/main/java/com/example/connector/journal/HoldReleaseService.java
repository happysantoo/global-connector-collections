package com.example.connector.journal;

import java.time.Instant;
import java.util.List;

/**
 * Hold and release: mark journal entries as held (connector_hold table);
 * release processes held items after held_until or on demand.
 */
public interface HoldReleaseService {

    /**
     * Mark a correlation ID as held until the given time.
     */
    void hold(String correlationId, Instant heldUntil, String reason);

    /**
     * Release a single correlation ID (process and remove from hold).
     * Returns true if was held and released.
     */
    boolean release(String correlationId);

    /**
     * List correlation IDs that are due for release (held_until <= now).
     */
    List<String> listDueForRelease();

    /**
     * Release all entries that are due (held_until <= now).
     */
    int releaseAllDue();
}
