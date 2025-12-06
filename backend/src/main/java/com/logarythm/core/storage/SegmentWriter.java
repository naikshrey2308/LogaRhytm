package com.logrhythm.core.storage;

import com.logrhythm.model.LogEntry;
import com.logrhythm.core.wal.CheckpointManager;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * SegmentWriter is responsible for writing flushed batches
 * of logs into long-term storage files ("segments").
 *
 * Segments differ from WAL:
 *  - WAL is append-only durability
 *  - Segments are the final, queryable storage format
 *
 * For now:
 *  - We store newline-delimited JSON
 *  - We create a new segment when size > 5MB
 *  - We update checkpoint so WAL cleanup can start working
 */
@Component
public class SegmentWriter {

    private static final String SEGMENT_DIR = "data/segments";
    private static final long MAX_SEGMENT_SIZE_BYTES = 1024 * 1024; // 1MB per segment

    private final CheckpointManager checkpointManager;

    private File currentSegmentFile;
    private FileOutputStream segmentStream;
    private int segmentIndex = 1;

    public SegmentWriter(CheckpointManager checkpointManager) throws IOException {
        this.checkpointManager = checkpointManager;

        initializeSegmentDirectory();
        openNewSegmentFile();
    }

    private void initializeSegmentDirectory() {
        File dir = new File(SEGMENT_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    private void openNewSegmentFile() throws IOException {
        String filename = String.format("segment-%06d.bin", segmentIndex);
        currentSegmentFile = new File(SEGMENT_DIR, filename);
        segmentStream = new FileOutputStream(currentSegmentFile, true);
        System.out.println("SegmentWriter: Opened new segment file " + filename);
    }

    private void rotateIfNeeded() throws IOException {
        if (currentSegmentFile.length() >= MAX_SEGMENT_SIZE_BYTES) {
            segmentStream.close();
            segmentIndex++;
            openNewSegmentFile();
        }
    }

    /**
     * Write flushed batch into the segment file.
     * For now, JSON lines are acceptable.
     *
     * Later we replace this with:
     *   - binary encoding
     *   - compression
     *   - indexed blocks
     */
    public synchronized void writeBatchToSegment(
            int walIndex,
            Iterable<LogEntry> batch
    ) throws IOException {

        rotateIfNeeded();

        for (LogEntry entry : batch) {
            String json = String.format(
                    "{\"timestamp\":%d,\"level\":\"%s\",\"message\":\"%s\"}\n",
                    entry.timestamp(),
                    entry.level(),
                    entry.message().replace("\"", "\\\"")
            );
            segmentStream.write(json.getBytes());
        }

        // THIS IS THE IMPORTANT PART:
        // We tell the checkpoint manager that WAL up to 'walIndex' is now safe.
        checkpointManager.updateCheckpoint(walIndex);

        System.out.println("SegmentWriter: Wrote batch from WAL index " + walIndex);
    }
}
