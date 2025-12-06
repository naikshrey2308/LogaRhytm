package com.logrhythm.core.storage;

import com.logrhythm.core.ingestion.IngestionService;
import com.logrhythm.model.LogEntry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * BatchFlusher subscribes to the ingestion pipeline and groups logs into batches.
 *
 * Responsibilities:
 * - Subscribe to the sink
 * - Collect logs into batches of 1000 OR flush every 1 second
 * - Call flushBatch(...) when a batch is ready
 * 
 */
@Component
public class BatchFlusher {

    private static final int BATCH_SIZE = 1000;

    private final IngestionService ingestionService;

    public BatchFlusher(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostConstruct
    public void startConsuming() {
        Flux<LogEntry> stream = ingestionService.getSink().asFlux();

        // Buffer 1000 logs OR flush every 1 second
        stream
            .bufferTimeout(BATCH_SIZE, Duration.ofSeconds(1))
            .filter(batch -> !batch.isEmpty())
            .subscribe(this::flushBatch);
    }

    /**
     * For now, just print the batch size.
     * Later, this will:
     * - append to WAL
     * - write to segment files
     * - update indexes
     */
    private void flushBatch(List<LogEntry> batch) {
        System.out.println("Flushed batch of size = " + batch.size());
    }
}
