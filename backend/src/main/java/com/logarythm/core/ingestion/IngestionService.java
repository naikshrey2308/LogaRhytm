package com.logrhythm.core.ingestion;

import com.logrhythm.model.LogEntry;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
/**
 * Async, thread-safe pipeline input, queue
*/ 
import reactor.core.publisher.Sinks;

/**
 * IngestionService is responsible only for accepting logs from the API
 * and pushing them into the internal reactive pipeline.
 * This is a reactive publisher source that downstream components consume from.
 */
@Service
public class IngestionService {

    // Acts like a buffered channel for downstream processing.
    private final Sinks.Many<LogEntry> sink;

    public IngestionService() {
        // Unicast ensures single consumer (our flusher).
        // Buffering is our backpressure strategy.
        this.sink = Sinks.many().unicast().onBackpressureBuffer();
    }

    /**
     * Accept a log entry and immediately push it to the sink.
     * The API will return 202 Accepted immediately.
     */
    public Mono<Void> accept(LogEntry entry) {
        sink.tryEmitNext(entry);
        return Mono.empty();
    }

    /**
     * Downstream components (like WAL writer, batch flusher, etc.)
     * will subscribe to this stream.
     */
    public Sinks.Many<LogEntry> getSink() {
        return sink;
    }
}
