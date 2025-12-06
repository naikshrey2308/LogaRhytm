package com.logrhythm.core.storage;

import com.logrhythm.core.ingestion.IngestionService;
import com.logrhythm.model.LogEntry;
import com.logrhythm.core.wal.WalWriter;
import com.logrhythm.core.wal.WalCleaner;
import com.logrhythm.core.wal.CheckpointManager;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.io.IOException;

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
    private final WalWriter walWriter;
    private final CheckpointManager checkpointManager;
    private final WalCleaner walCleaner;

    public BatchFlusher(IngestionService ingestionService, 
                        WalWriter walWriter, 
                        CheckpointManager checkpointManager,
                        WalCleaner walCleaner) {
        this.ingestionService = ingestionService;
        this.walWriter = walWriter;
        this.checkpointManager = checkpointManager;
        this.walCleaner = walCleaner;
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
        try {
            for (LogEntry entry : batch) {
                walWriter.append(entry);
            }

            System.out.println("WAL: wrote batch of size " + batch.size());

            // TODO: When segment dumper writes segments, update and cleanup
            // checkpointManager.updateLastFlushedWalIndex(...);
            // walCleaner.cleanupOldWals(...);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
