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

    @GetMapping("/logs")
    public List<LogEntry> getLastLogs(@RequestParam(defaultValue = "100") int limit) throws Exception {
        return segmentReader.readLastNLogs(limit);
    }
}
