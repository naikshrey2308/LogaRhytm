package com.logrhythm.core.storage;

import com.logrhythm.model.LogEntry;
import com.logrhythm.core.wal.CheckpointManager;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
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
    private static final long MAX_SEGMENT_SIZE_BYTES = 10 * 1024 * 1024; // 1MB per segment

    private final CheckpointManager checkpointManager;

    private File currentSegmentFile;
    private BufferedOutputStream segmentStream;
    private FileOutputStream fileStream;
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

        fileStream = new FileOutputStream(currentSegmentFile, true);
        segmentStream = new BufferedOutputStream(fileStream, 64 * 1024);

        System.out.println("SegmentWriter: Opened new segment file " + filename);
    }

    private void rotateIfNeeded() throws IOException {
        if (currentSegmentFile.length() >= MAX_SEGMENT_SIZE_BYTES) {
            segmentStream.flush();
            segmentStream.close();
            fileStream.close();

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
    public synchronized void writeBatchToSegment(int walIndex, Iterable<LogEntry> batch) throws IOException {
        rotateIfNeeded();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(64 * 1024);

        for (LogEntry entry : batch) {
            byte[] msgBytes = entry.message().getBytes("UTF-8");
            int msgLen = msgBytes.length;

            buffer.write(longToBytes(entry.timestamp()));
            buffer.write(entry.levelAsByte());
            buffer.write(shortToBytes((short) msgLen));
            buffer.write(msgBytes);
        }

        // ONE write instead of 4000
        segmentStream.write(buffer.toByteArray());

        // update checkpoint
        checkpointManager.updateCheckpoint(walIndex);
    }

    /**
     * Helper Functions
     */
        /**
     * Helper Functions
     */
    private byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }

    private byte[] shortToBytes(short value) {
        return new byte[] {
            (byte) ((value >> 8) & 0xFF),
            (byte) (value & 0xFF)
        };
    }
}
