package com.logrhythm.core.wal;

import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class WalCleaner {

    private static final String WAL_DIR = "data/wal";

    public void cleanupOldWals(int checkpointIndex) {
        File dir = new File(WAL_DIR);
        if (!dir.exists()) return;

        File[] files = dir.listFiles((d, name) -> name.startsWith("wal-") && name.endsWith(".log"));
        if (files == null) return;

        for (File f : files) {
            int index = extractIndex(f.getName());
            if (index <= checkpointIndex) {
                System.out.println("WAL Cleaner: Deleting old WAL " + f.getName());
                f.delete();
            }
        }
    }

    private int extractIndex(String filename) {
        // filename format: wal-000123.log
        try {
            return Integer.parseInt(filename.substring(4, 10)); 
        } catch (Exception e) {
            return -1;
        }
    }
}