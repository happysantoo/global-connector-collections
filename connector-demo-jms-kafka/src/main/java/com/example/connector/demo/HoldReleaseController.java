package com.example.connector.demo;

import com.example.connector.journal.HoldReleaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for hold/release. Demonstrates hold and release non-functional requirement.
 */
@RestController
@RequestMapping("/connector/hold")
public class HoldReleaseController {

    private final HoldReleaseService holdReleaseService;

    public HoldReleaseController(HoldReleaseService holdReleaseService) {
        this.holdReleaseService = holdReleaseService;
    }

    @PostMapping("/{correlationId}")
    public ResponseEntity<String> hold(
            @PathVariable String correlationId,
            @RequestBody(required = false) Map<String, Object> body) {
        Instant heldUntil = body != null && body.containsKey("heldUntil")
                ? Instant.parse(body.get("heldUntil").toString())
                : Instant.now().plusSeconds(60);
        String reason = body != null && body.containsKey("reason") ? body.get("reason").toString() : "manual";
        holdReleaseService.hold(correlationId, heldUntil, reason);
        return ResponseEntity.accepted().body("Held: " + correlationId + " until " + heldUntil);
    }

    @PostMapping("/{correlationId}/release")
    public ResponseEntity<String> release(@PathVariable String correlationId) {
        boolean released = holdReleaseService.release(correlationId);
        return released
                ? ResponseEntity.ok("Released: " + correlationId)
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/due")
    public List<String> listDue() {
        return holdReleaseService.listDueForRelease();
    }

    @PostMapping("/release-due")
    public ResponseEntity<String> releaseAllDue() {
        int count = holdReleaseService.releaseAllDue();
        return ResponseEntity.ok("Released " + count + " due");
    }
}
