package com.logarythm.model;

import java.util.List;

public record QueryResponse(
        long tookMs,
        int count,
        List<LogEntry> results) {
}
