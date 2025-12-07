package com.logarythm.core.wal;

import com.logarythm.core.storage.SegmentWriter;
import com.logarythm.model.LogEntry;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * WAL Replayer:
 * Runs on startup and replays WAL files that contain logs
 * which were not flushed into segment storage before a crash.
 *
 * Replay rules:
 *  - Read checkpoint: lastFlushedWalIndex
 *  - Replay WAL files > checkpoint
 *  - Write their logs into SegmentWriter
 *  - Update checkpoint
 *  - Run WalCleaner
 */
@Component
public class WalReplayer {

    private static final String WAL_DIR = "data/wal";

    private final CheckpointManager checkpointManager;
    private final SegmentWriter segmentWriter;
    private final WalCleaner walCleaner;

    public WalReplayer(CheckpointManager checkpointManager,
                       SegmentWriter segmentWriter,
                       WalCleaner walCleaner) {
        this.checkpointManager = checkpointManager;
        this.segmentWriter = segmentWriter;
        this.walCleaner = walCleaner;
    }

    private LogEntry readBinaryRecord(InputStream in) throws IOException {
        byte[] tsBytes = in.readNBytes(8);
        if (tsBytes.length < 8) return null; // EOF
        long timestamp = bytesToLong(tsBytes);

        int levelByte = in.read();
        if (levelByte < 0) return null;

        byte[] lenBytes = in.readNBytes(2);
        if (lenBytes.length < 2) return null;
        int msgLen = bytesToShort(lenBytes);

        byte[] msgBytes = in.readNBytes(msgLen);
        if (msgBytes.length < msgLen) return null;

        return new LogEntry(timestamp, decodeLevel(levelByte), new String(msgBytes, "UTF-8"));
    }

    @PostConstruct
    public void replayPendingWalFiles() {
        int lastFlushed = checkpointManager.getLastFlushedWalIndex();

        File dir = new File(WAL_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("wal-") && name.endsWith(".log"));

        if (files == null) return;

        System.out.println("WalReplayer: Starting replay. lastFlushedWalIndex=" + lastFlushed);

        List<File> toReplay = new ArrayList<>();

        for (File file : files) {
            int index = extractIndex(file.getName());
            if (index > lastFlushed) {
                toReplay.add(file);
            }
        }

        // Sort by index (in case OS lists files out of order)
        toReplay.sort((a, b) -> extractIndex(a.getName()) - extractIndex(b.getName()));

        for (File file : toReplay) {
            int walIndex = extractIndex(file.getName());
            replaySingleFile(file, walIndex);
        }

        // After replaying → clean WAL files
        walCleaner.cleanOldWalFiles();

        System.out.println("WalReplayer: Replay complete.");
    }

    private void replaySingleFile(File file, int walIndex) {
        System.out.println("WalReplayer: Replaying " + file.getName());

        try (InputStream in = new FileInputStream(file)) {
            List<LogEntry> batch = new ArrayList<>();

            LogEntry entry;
            while ((entry = readBinaryRecord(in)) != null) {
                batch.add(entry);
            }

            if (!batch.isEmpty()) {
                segmentWriter.writeBatchToSegment(walIndex, batch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int extractIndex(String filename) {
        try {
            return Integer.parseInt(filename.substring(4, 10)); // wal-000123.log
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Helper Functions
     */
    private long bytesToLong(byte[] b) {
        return ((long)(b[0] & 0xff) << 56) |
            ((long)(b[1] & 0xff) << 48) |
            ((long)(b[2] & 0xff) << 40) |
            ((long)(b[3] & 0xff) << 32) |
            ((long)(b[4] & 0xff) << 24) |
            ((long)(b[5] & 0xff) << 16) |
            ((long)(b[6] & 0xff) << 8)  |
            ((long)(b[7] & 0xff));
    }

    private short bytesToShort(byte[] b) {
        return (short)(((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
    }

    private String decodeLevel(int lvl) {
        return switch (lvl) {
            case 1 -> "INFO";
            case 2 -> "WARN";
            case 3 -> "ERROR";
            case 4 -> "DEBUG";
            default -> "UNKNOWN";
        };
    }

}
