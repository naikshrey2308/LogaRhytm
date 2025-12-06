package com.logrhythm.core.wal;

import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class WalCleaner {

    private static final String WAL_DIR = "data/wal";

    private final CheckpointManager checkpointManager;

    public WalCleaner(CheckpointManager checkpointManager) {
        this.checkpointManager = checkpointManager;
    }

    /**
     * Deletes WAL files whose index <= lastFlushedWalIndex.
     */
    public void cleanOldWalFiles() {
        int checkpoint = checkpointManager.getLastFlushedWalIndex();

        File dir = new File(WAL_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("wal-") && name.endsWith(".log"));

        if (files == null) return;

        for (File f : files) {
            int fileIndex = extractIndex(f.getName());

            if (fileIndex <= checkpoint) {
                boolean deleted = f.delete();
                if (deleted) {
                    System.out.println("WalCleaner: deleted " + f.getName());
                } else {
                    System.out.println("WalCleaner: FAILED to delete " + f.getName());
                }
            }
        }
    }

    private int extractIndex(String filename) {
        // wal-000123.log → 123
        try {
            return Integer.parseInt(filename.substring(4, 10));
        } catch (Exception e) {
            return -1;
        }
    }
}