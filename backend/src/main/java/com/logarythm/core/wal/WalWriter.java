package com.logrhythm.core.wal;

import com.logrhythm.model.LogEntry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.List;

/**
 * WAL = Write Ahead Log
 * Responsible for durable append-only logging before segment writes.
 */
@Component
public class WalWriter {

    private static final String WAL_DIR = "data/wal";
    private static final long MAX_WAL_SIZE_BYTES = 1024 * 1024; // 1 MB per WAL log file

    private File currentWalFile;
    private BufferedOutputStream walStream;
    private FileOutputStream fileStream;
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

        fileStream = new FileOutputStream(currentWalFile, true);
        walStream = new BufferedOutputStream(fileStream, 64 * 1024); // 64KB buffer

        System.out.println("WAL: Opened new WAL file " + filename);
    }

    private void rotateIfNeeded() throws IOException {
        if (currentWalFile.length() >= MAX_WAL_SIZE_BYTES) {
            walStream.flush();
            walStream.close();
            fileStream.close();
            walIndex++;
            openNewWalFile();
        }
    }

    public synchronized void appendBatch(List<LogEntry> batch) throws IOException {
        rotateIfNeeded();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(batch.size() * 64);

        for (LogEntry entry : batch) {
            byte[] msgBytes = entry.message().getBytes("UTF-8");
            int msgLen = msgBytes.length;

            buffer.write(longToBytes(entry.timestamp()));
            buffer.write(entry.levelAsByte());
            buffer.write(shortToBytes((short) msgLen));
            buffer.write(msgBytes);
        }

        walStream.write(buffer.toByteArray());
    }

    /**
     * Append log to WAL (JSON lines for now).
     */
    public synchronized void append(LogEntry entry) throws IOException {
        rotateIfNeeded();

        byte[] msgBytes = entry.message().getBytes("UTF-8");
        int msgLen = msgBytes.length;

        // Write binary record
        walStream.write(longToBytes(entry.timestamp()));
        walStream.write(entry.levelAsByte());
        walStream.write(shortToBytes((short) msgLen));
        walStream.write(msgBytes);
    }

    /**
     * Expose current WAL file index — used for checkpointing.
     */
    public int getCurrentWalIndex() {
        return walIndex;
    }

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
