package com.logrhythm.api;

import com.logrhythm.model.LogEntry;

import java.util.List;

public record QueryResponse(
        long tookMs,
        int count,
        List<LogEntry> results) {
}
