package com.logrhythm.core.wal;

import com.logrhythm.model.LogEntry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * WAL = Write Ahead Log
 * WalWriter is responsible for writing all incoming logs
 * to an append-only Write-Ahead Log file for durability.
 *
 * This is a minimal implementation:
 * - Creates the WAL directory if missing
 * - Opens/creates wal-000001.log
 * - Appends raw log data (newline-delimited JSON for now)
 * - Handles basic file rotation when size exceeds threshold
 */
@Component
public class WalWriter {

    private static final String WAL_DIR = "data/wal";
    private static final long MAX_WAL_SIZE_BYTES = 100 * 1024; // 100 KB

    private File currentWalFile;
    private FileOutputStream walStream;
    private int walIndex = 1;

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
     * Append a log entry to the WAL.
     * For now, we serialize it as newline-delimited JSON.
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
}
