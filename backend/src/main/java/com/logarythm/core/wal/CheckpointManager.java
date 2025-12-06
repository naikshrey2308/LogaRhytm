package com.logrhythm.core.wal;

import org.springframework.stereotype.Component;

import java.io.*;

/**
 * Tracks which WAL files are safely flushed into segment storage.
 * 
 * Stored in data/wal/checkpoint.meta:
 * 
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

    /**
     * Update checkpoint when segment flush succeeds.
     */
    public synchronized void updateCheckpoint(int index) {
        this.lastFlushedWalIndex = index;
        saveCheckpoint();
    }

    private void loadCheckpoint() {
        File file = new File(CHECKPOINT_FILE);

        if (!file.exists()) {
            System.out.println("CheckpointManager: No checkpoint found. Starting fresh.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();

            if (line != null && line.startsWith("lastFlushedWalIndex=")) {
                this.lastFlushedWalIndex = Integer.parseInt(line.split("=")[1]);
            }

            System.out.println("CheckpointManager: Loaded checkpoint → " + lastFlushedWalIndex);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveCheckpoint() {
        File file = new File(CHECKPOINT_FILE);
        file.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write("lastFlushedWalIndex=" + lastFlushedWalIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("CheckpointManager: Updated checkpoint → " + lastFlushedWalIndex);
    }
}
