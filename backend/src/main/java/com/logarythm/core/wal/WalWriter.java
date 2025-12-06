package com.logrhythm.core.wal;

import com.logrhythm.model.LogEntry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * WAL = Write Ahead Log
 * Responsible for durable append-only logging before segment writes.
 */
@Component
public class WalWriter {

    private static final String WAL_DIR = "data/wal";
    private static final long MAX_WAL_SIZE_BYTES = 200 * 1024; // 200 KB

    private File currentWalFile;
    private FileOutputStream walStream;
    private int walIndex = 1;  // used by SegmentWriter + CheckpointManager

    public WalWriter() throws IOException {
        initializeWalDirectory();
        openNewWalFile();
    }

    private void initializeWalDirectory() {
        File dir = new File(WAL_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void openNewWalFile() throws IOException {
        String filename = String.format("wal-%06d.log", walIndex);
        currentWalFile = new File(WAL_DIR, filename);
        walStream = new FileOutputStream(currentWalFile, true);
        System.out.println("WAL: Opened new WAL file " + currentWalFile.getName());
    }

    private void rotateIfNeeded() throws IOException {
        if (currentWalFile.length() >= MAX_WAL_SIZE_BYTES) {
            walStream.close();
            walIndex++;
            openNewWalFile();
        }
    }

    /**
     * Append log to WAL (JSON lines for now).
     */
    public synchronized void append(LogEntry entry) throws IOException {
        rotateIfNeeded();

        String json = String.format(
                "{\"timestamp\":%d,\"level\":\"%s\",\"message\":\"%s\"}\n",
                entry.timestamp(),
                entry.level(),
                entry.message().replace("\"", "\\\"")
        );

        walStream.write(json.getBytes());
    }

    /**
     * Expose current WAL file index — used for checkpointing.
     */
    public int getCurrentWalIndex() {
        return walIndex;
    }
}
