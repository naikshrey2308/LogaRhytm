package com.logrhythm.core.wal;

import org.springframework.stereotype.Component;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages checkpoint metadata for WAL cleanup and replay.
 * 
 * checkpoint.meta file structure:
 * lastFlushedWalIndex=5
 */
@Component
public class CheckpointManager {

    private static final String CHECKPOINT_FILE = "data/wal/checkpoint.meta";

    private int lastFlushedWalIndex = 0;

    public CheckpointManager() {
        loadCheckpoint();
    }

    public int getLastFlushedWalIndex() {
        return lastFlushedWalIndex;
    }

    public synchronized void updateLastFlushedWalIndex(int index) {
        this.lastFlushedWalIndex = index;
        saveCheckpoint();
    }

    private void loadCheckpoint() {
        File file = new File(CHECKPOINT_FILE);
        if (!file.exists()) {
            return; // No checkpoint yet = start from zero
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && line.startsWith("lastFlushedWalIndex=")) {
                this.lastFlushedWalIndex = Integer.parseInt(line.split("=")[1]);
            }
            System.out.println("Checkpoint loaded: lastFlushedWalIndex=" + lastFlushedWalIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveCheckpoint() {
        File file = new File(CHECKPOINT_FILE);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("lastFlushedWalIndex=" + lastFlushedWalIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Checkpoint updated → lastFlushedWalIndex=" + lastFlushedWalIndex);
    }
}
