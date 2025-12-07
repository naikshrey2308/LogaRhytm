package com.logrhythm.api;

import com.logrhythm.core.storage.SegmentReader;
import com.logrhythm.model.LogEntry;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class QueryController {

    private final SegmentReader segmentReader;

    public QueryController(SegmentReader segmentReader) {
        this.segmentReader = segmentReader;
    }

    @GetMapping("/query")
    public QueryResponse queryLogs(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String message) throws Exception {

        long t0 = System.currentTimeMillis();

        List<LogEntry> results = segmentReader.queryLogs(
                limit,
                start,
                end,
                level,
                message);

        long took = System.currentTimeMillis() - t0;

        return new QueryResponse(took, results.size(), results);
    }
}