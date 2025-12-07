package com.logrhythm.core.storage;

import com.logrhythm.model.LogEntry;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SegmentReader {

    private static final String SEGMENT_DIR = "data/segments";

    public List<LogEntry> readLastNLogs(int limit) throws IOException {
        File dir = new File(SEGMENT_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("segment-") && name.endsWith(".bin"));
        if (files == null || files.length == 0) {
            return List.of();
        }

        // newest segment first
        List<File> segments = new ArrayList<>(List.of(files));
        segments.sort((a, b) -> b.getName().compareTo(a.getName()));

        List<LogEntry> collected = new ArrayList<>();

        for (File file : segments) {
            if (collected.size() >= limit)
                break; // already have enough
            readSegmentForward(file, collected, limit);
        }

        // Sort by timestamp descending (newest first)
        collected.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));

        // Return only the last N newest logs
        return collected.size() > limit ? collected.subList(0, limit) : collected;
    }

    private void readSegmentForward(File file, List<LogEntry> out, int limit) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(fis);

        while (dis.available() > 0 && out.size() < limit) {

            long timestamp = dis.readLong();
            byte lvl = dis.readByte();
            int msgLen = dis.readUnsignedShort();

            byte[] msg = new byte[msgLen];
            dis.readFully(msg);

            out.add(new LogEntry(timestamp, decodeLevel(lvl), new String(msg, "UTF-8")));

            // STOP immediately when limit hit
            if (out.size() >= limit) {
                break;
            }
        }

        dis.close();
    }

    private String decodeLevel(byte lvl) {
        return switch (lvl) {
            case 1 -> "INFO";
            case 2 -> "WARN";
            case 3 -> "ERROR";
            case 4 -> "DEBUG";
            default -> "UNKNOWN";
        };
    }
}