package com.logarythm.core.bloom;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Component
public class BloomFilterManager {

    private static final String SEGMENT_DIR = "data/segments";

    private final Map<String, SegmentBloom> bloomMap = new HashMap<>();

    public BloomFilterManager() {
        loadAllBlooms();
    }

    private void loadAllBlooms() {
        File dir = new File(SEGMENT_DIR);
        File[] bfs = dir.listFiles((d, name) -> name.endsWith(".bf"));

        if (bfs == null)
            return;

        for (File f : bfs) {
            try {
                BloomFilter bf = BloomFilter.loadFromFile(f);
                SegmentBloom sb = new SegmentBloom(bf);

                // restore timestamps from filename if desired later
                bloomMap.put(f.getName().replace(".bf", ""), sb);
            } catch (Exception ignored) {
            }
        }
    }

    public SegmentBloom get(String segmentName) {
        return bloomMap.get(segmentName);
    }

    public void register(String segmentName, SegmentBloom sb) {
        bloomMap.put(segmentName, sb);
    }
}