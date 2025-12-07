package com.logarythm.core.bloom;

public class SegmentBloom {
    public final BloomFilter filter;
    public long minTs = Long.MAX_VALUE;
    public long maxTs = Long.MIN_VALUE;

    public SegmentBloom(BloomFilter filter) {
        this.filter = filter;
    }

    public void updateTs(long ts) {
        minTs = Math.min(minTs, ts);
        maxTs = Math.max(maxTs, ts);
    }
}