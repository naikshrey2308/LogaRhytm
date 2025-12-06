package com.logrhythm.core.storage;

import com.logrhythm.core.ingestion.IngestionService;
import com.logrhythm.model.LogEntry;
import com.logrhythm.core.wal.WalWriter;
import com.logrhythm.core.wal.WalCleaner;
import com.logrhythm.core.wal.CheckpointManager;
import com.logrhythm.core.storage.SegmentWriter;
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
    private final SegmentWriter segmentWriter;

    public BatchFlusher(IngestionService ingestionService,
                        WalWriter walWriter,
                        WalCleaner walCleaner,
                        CheckpointManager checkpointManager,
                        SegmentWriter segmentWriter) {
        this.ingestionService = ingestionService;
        this.walWriter = walWriter;
        this.checkpointManager = checkpointManager;
        this.segmentWriter = segmentWriter;
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
     * - deletes old WAL files
     */
    private void flushBatch(List<LogEntry> batch) {
        try {
            int walIndex = walWriter.getCurrentWalIndex();

            // 1. Write WAL entries
            walWriter.appendBatch(batch);

            // 2. Write to segment storage
            segmentWriter.writeBatchToSegment(walIndex, batch);

            // 3. CLEANUP OLD WAL FILES
            walCleaner.cleanOldWalFiles();

            System.out.println("BatchFlusher: wrote batch=" + batch.size() + " walIndex=" + walIndex);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
