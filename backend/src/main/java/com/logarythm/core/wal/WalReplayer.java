package com.logrhythm.core.wal;

import com.logrhythm.core.storage.SegmentWriter;
import com.logrhythm.model.LogEntry;
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

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            List<LogEntry> batch = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                LogEntry entry = parseJsonLine(line);
                batch.add(entry);
            }

            if (!batch.isEmpty()) {
                segmentWriter.writeBatchToSegment(walIndex, batch);
                System.out.println("WalReplayer: Replayed " + batch.size() + " logs from " + file.getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LogEntry parseJsonLine(String json) {
        // extremely simple JSON parsing for now
        long timestamp = Long.parseLong(json.split("\"timestamp\":")[1].split(",")[0]);
        String level = json.split("\"level\":\"")[1].split("\"")[0];
        String message = json.split("\"message\":\"")[1].split("\"")[0];
        return new LogEntry(timestamp, level, message);
    }

    private int extractIndex(String filename) {
        try {
            return Integer.parseInt(filename.substring(4, 10)); // wal-000123.log
        } catch (Exception e) {
            return -1;
        }
    }
}
