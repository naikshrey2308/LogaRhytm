package com.logrhythm.model;

/**
 * LogEntry represents a single log event ingested by LogRhythm.
 * It is intentionally minimal; other layers (WAL, encoding, indexing)
 * will work around this immutable record.
 */
public record LogEntry(
        long timestamp,
        String level,
        String message
) {}
