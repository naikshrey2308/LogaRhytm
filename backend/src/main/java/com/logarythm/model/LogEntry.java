package com.logrhythm.model;

/**
 * LogEntry represents a single log event ingested by LogRhythm.
 * It is intentionally minimal; other layers (WAL, encoding, indexing)
 * will work around this immutable record.
 */
public record LogEntry(long timestamp, String level, String message) {

    public byte levelAsByte() {
        return switch (level.toUpperCase()) {
            case "INFO" -> 1;
            case "WARN" -> 2;
            case "ERROR" -> 3;
            case "DEBUG" -> 4;
            default -> 0;
        };
    }
}