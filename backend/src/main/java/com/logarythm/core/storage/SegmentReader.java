package com.logarythm.core.storage;

import com.logarythm.core.bloom.BloomFilterManager;
import com.logarythm.core.bloom.SegmentBloom;
import com.logarythm.model.LogEntry;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class SegmentReader {

    private static final String SEGMENT_DIR = "data/segments";

    private final BloomFilterManager bloomManager;

    public SegmentReader(BloomFilterManager bloomManager) {
        this.bloomManager = bloomManager;
    }

    /**
     * Unified log reader: applies limit, time range, level, and keyword search.
     * Bloom Filters are used to SKIP entire segments cheaply.
     */
    public List<LogEntry> queryLogs(
            Integer limit,
            Long start,
            Long end,
            String levelFilter,
            String messageKeyword) throws IOException {

        if (limit == null || limit <= 0)
            limit = 100;

        if (messageKeyword != null)
            messageKeyword = messageKeyword.toLowerCase();

        File dir = new File(SEGMENT_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("segment-") && name.endsWith(".bin"));

        if (files == null || files.length == 0)
            return List.of();

        // newest → oldest
        List<File> segments = new ArrayList<>(List.of(files));
        segments.sort((a, b) -> b.getName().compareTo(a.getName()));

        List<LogEntry> collected = new ArrayList<>(limit);

        for (File seg : segments) {
            if (collected.size() >= limit)
                break;

            if (!segmentLikelyMatches(seg, start, end, levelFilter, messageKeyword))
                continue; // bloom filter rejects → skip

            readSegment(seg, collected, limit, start, end, levelFilter, messageKeyword);
        }

        // Sort descending by timestamp
        collected.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));

        if (collected.size() > limit)
            return collected.subList(0, limit);

        return collected;
    }

    /**
     * Use Bloom Filter + metadata to skip entire segments cheaply.
     */
    private boolean segmentLikelyMatches(
            File seg,
            Long start,
            Long end,
            String levelFilter,
            String keyword) {

        String baseName = seg.getName().replace(".bin", "");
        SegmentBloom sb = bloomManager.get(baseName);

        if (sb == null) {
            // no bloom filter means we must scan
            return true;
        }

        // time-range skip
        if (start != null && sb.maxTs < start)
            return false;

        if (end != null && sb.minTs > end)
            return false;

        // level skip
        if (levelFilter != null && !sb.filter.mightContain(levelFilter))
            return false;

        // keyword skip
        if (keyword != null && !sb.filter.mightContain(keyword))
            return false;

        return true;
    }

    /**
     * Binary segment scanner — safe, EOF-resistant.
     */
    private void readSegment(
            File file,
            List<LogEntry> out,
            int limit,
            Long start,
            Long end,
            String levelFilter,
            String keyword) throws IOException {

        FileInputStream fis = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(fis);

        try {

            while (out.size() < limit) {

                // Need at least 11 bytes for a complete header
                if (dis.available() < 11)
                    break;

                long ts = dis.readLong();
                byte lvlByte = dis.readByte();
                int msgLen = dis.readUnsignedShort();

                if (msgLen < 0 || msgLen > 10000)
                    break; // corruption or leftover bytes

                if (dis.available() < msgLen)
                    break;

                byte[] msgBytes = new byte[msgLen];
                dis.readFully(msgBytes);

                String msg = new String(msgBytes, StandardCharsets.UTF_8);
                String lvl = decodeLevel(lvlByte);

                // apply filters
                if (start != null && ts < start)
                    continue;
                if (end != null && ts > end)
                    continue;
                if (levelFilter != null && !lvl.equalsIgnoreCase(levelFilter))
                    continue;
                if (keyword != null && !msg.toLowerCase().contains(keyword))
                    continue;

                out.add(new LogEntry(ts, lvl, msg));
            }

        } finally {
            dis.close();
        }
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
