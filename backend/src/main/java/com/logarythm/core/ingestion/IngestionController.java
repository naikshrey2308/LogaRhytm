package com.logarythm.core.ingestion;

import com.logarythm.model.LogEntry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * The API entrypoint for ingesting logs into LogRhythm.
 *
 * Responsibilities:
 * - Accept HTTP POST requests
 * - Deserialize JSON into a LogEntry
 * - Forward the entry to IngestionService
 * 
 */
@RestController
@RequestMapping("/ingest")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Accept a log entry via HTTP:
     * POST /ingest
     *
     * Expected JSON:
     * { "timestamp": 123, "level": "INFO", "message": "something" }
     *
     * Returns 202 Accepted immediately after pushing to the ingestion pipeline.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> ingest(@RequestBody Mono<LogEntry> logMono) {
        return logMono.flatMap(ingestionService::accept);
    }
}