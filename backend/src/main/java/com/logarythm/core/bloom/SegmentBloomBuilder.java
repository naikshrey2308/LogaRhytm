package com.logarythm.core.bloom;

import com.logarythm.model.LogEntry;

/**
 * Builds a SegmentBloom object for one segment file.
 * 
 * Responsibilities:
 * - Add log tokens to bloom filter
 * - Add log levels to bloom filter
 * - Track min/max timestamps
 */
public class SegmentBloomBuilder {

    private final SegmentBloom sb;

    public SegmentBloomBuilder(int bitSize, int numHashes) {
        this.sb = new SegmentBloom(new BloomFilter(bitSize, numHashes));
    }

    public void add(LogEntry e) {
        if (e == null)
            return;

        // add level
        sb.filter.add(e.level());
        sb.updateTs(e.timestamp());

        // tokenize message by whitespace
        String msg = e.message();
        if (msg != null) {
            String[] tokens = msg.split("\\s+");
            for (String t : tokens) {
                if (!t.isBlank()) {
                    sb.filter.add(t.toLowerCase());
                }
            }
        }
    }

    public SegmentBloom build() {
        return sb;
    }
}
