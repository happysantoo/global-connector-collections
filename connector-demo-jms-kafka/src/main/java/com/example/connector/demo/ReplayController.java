package com.example.connector.demo;

import com.example.connector.core.transport.SendResult;
import com.example.connector.journal.ReplayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoint to replay by correlation_id. Demonstrates replay non-functional requirement.
 */
@RestController
@RequestMapping("/connector/replay")
public class ReplayController {

    private final ReplayService replayService;

    public ReplayController(ReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/{correlationId}")
    public ResponseEntity<String> replay(
            @PathVariable String correlationId,
            @RequestBody(required = false) Map<String, Object> options) {
        return replayService.replay(correlationId, options != null ? options : Map.of())
                .map(future -> {
                    try {
                        SendResult result = future.get();
                        return result instanceof SendResult.Success
                                ? ResponseEntity.accepted().body("Replay submitted: " + correlationId)
                                : ResponseEntity.status(500).body("Replay failed: " + ((SendResult.Failure) result).cause().getMessage());
                    } catch (Exception e) {
                        return ResponseEntity.status(500).body("Replay error: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
