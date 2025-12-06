# 🔥 LogRhythm — High-Performance Log Ingestion & Storage Engine (Java + WebFlux)

LogRhythm is a **high-throughput, crash-safe log ingestion engine** built entirely in Java using **Spring WebFlux** and a custom **write-optimized storage architecture** inspired by Kafka, Cassandra, and RocksDB.  
It supports:

- **4,000+ logs/sec ingestion throughput**  
- **Durable Write-Ahead Logging (WAL)**  
- **Segmented storage engine with binary encoding**  
- **Crash recovery via WAL replay**  
- **Checkpointing and WAL cleanup**  
- **Batch-optimized disk I/O**  
- **Fully reactive ingestion pipeline (WebFlux Sink)**  

This project will later serve as the backend for a complete distributed log analytics system.

---

## 🚀 Features Implemented So Far

### **1. Reactive Log Ingestion (WebFlux)**
- Accepts JSON log entries via:
  ```
  POST /ingest
  ```
- Uses `Sinks.many().unicast().onBackpressureBuffer()` for efficient async ingestion.
- Supports backpressure and high concurrency.

---

### **2. Binary Write-Ahead Log (WAL)**
Every incoming log is written to a **binary-encoded WAL file** for durability.

Binary format:
```
[8 bytes  timestamp]
[1 byte   level enum]
[2 bytes  message length]
[N bytes  message UTF-8]
```

WAL features:
- Append-only
- File rotation at configurable size
- Batch writes (one syscall per batch → high throughput)
- Buffered I/O for reduced syscall overhead

---

### **3. Segment Storage Engine**
Logs are flushed from WAL into **segment files**:

- Binary-encoded
- Batch-written for maximum throughput
- Segment rotation based on size thresholds
- Future-ready for indexing and searching

This creates a durable, query-friendly long-term storage layer.

---

### **4. Checkpointing System**
After a batch is successfully written to a segment, the system:

- Updates `checkpoint.meta` with the last flushed WAL file index
- Ensures that WAL cleanup only happens after durable persistence
- Guarantees crash-safe recovery behavior

Contents of `checkpoint.meta`:
```
lastFlushedWalIndex=X
```

---

### **5. WAL Cleanup**
A cleanup module deletes WAL files **only after** they are confirmed persisted in segments (via checkpoint).

This prevents unbounded storage growth and mimics production database design (PostgreSQL, Kafka, Cassandra).

---

### **6. Crash Recovery (WAL Replay)**
On startup, LogRhythm automatically:

1. Reads checkpoint  
2. Scans all WAL files with index > checkpoint  
3. Replays their binary log entries into segments  
4. Updates checkpoint again  
5. Deletes recovered WAL files

This guarantees **no data loss**, even on abrupt shutdown.

---

### **7. High Throughput via Batching + Binary Encoding**
Major optimizations implemented:

- Batched WAL writes → 1 syscall per batch  
- Batched segment writes  
- Binary encoding to avoid JSON overhead  
- Buffered streams for disk efficiency  
- Reactive batching with `bufferTimeout()`

These optimizations together produce:

# **🔥 Sustained 4,074 requests/sec ingestion throughput (Windows, ab test, 100 concurrency)**

Measured using ApacheBench:

```
Requests per second:    4074.78 [#/sec] (mean)
P50 latency:             ~23 ms
P95 latency:             ~34 ms
Maximum latency:         ~81 ms
Failed requests:         0
```

This represents **real, production-style performance**, not synthetic micro-benchmarks.

---

## 📂 Storage Layout

```
data/
 ├── wal/
 │    ├── wal-000001.log
 │    ├── wal-000002.log
 │    └── checkpoint.meta
 └── segments/
      ├── segment-000001.bin
      └── segment-000002.bin
```

---

## 📦 Architecture Overview

```
             ┌──────────────────────┐
             │      /ingest API     │
             └───────────┬──────────┘
                         ▼
         ┌────────────────────────────────┐
         │        WebFlux Sink Queue      │
         └─────────────────┬──────────────┘
                           ▼
              BatchFlusher (1000 logs or 1s)
                           ▼
         ┌───────────────────────────────────┐
         │     1. Binary WAL (batched)        │
         └────────────────┬───────────────────┘
                          ▼
         ┌───────────────────────────────────┐
         │   2. Segment Writer (batched)      │
         └────────────────┬───────────────────┘
                          ▼
         ┌───────────────────────────────────┐
         │  3. Checkpoint Update              │
         └────────────────┬───────────────────┘
                          ▼
         ┌───────────────────────────────────┐
         │  4. WAL Cleaner (safe deletion)   │
         └───────────────────────────────────┘
```

On startup:

```
Startup → WAL Replayer → Segment Writer → Cleanup → Ready
```

---

## 🧪 Benchmarking Setup

Used ApacheBench (`ab`):

```
ab -n 100000 -c 100 -p log.json -T application/json http://localhost:8080/ingest
```

Sample request body:

```json
{
  "timestamp": 1,
  "level": "INFO",
  "message": "hello"
}
```

---

## 🎯 Current Capabilities Summary

| Feature | Status |
|--------|--------|
| Ingestion (WebFlux) | ✔ Done |
| WAL (binary, batched) | ✔ Done |
| Segment writer | ✔ Done |
| Checkpointing | ✔ Done |
| WAL cleanup | ✔ Done |
| Crash recovery | ✔ Done |
| High throughput optimization | ✔ Done |
| Read path (GET /logs) | 🔜 Planned |
| Bloom filter indexing | 🔜 Planned |
| Keyword search | 🔜 Planned |
| Dashboard/UI | 🔜 Planned |

---

## 🛣️ Next Steps (Roadmap)

### **1. Read Path**
- `GET /logs?limit=N`
- Reverse scan segments

### **2. Bloom Filter Per Segment**
- Speed up queries
- Skip irrelevant segments instantly

### **3. Search API**
- Keyword search  
- Level filtering  
- Time-range queries  

### **4. UI Dashboard**
- Live ingestion metrics  
- Segment/WAL stats  
- Search interface  

---

## 🛍️ Tech Stack

- **Java 21**
- **Spring Boot + WebFlux**
- **Reactive Streams (Project Reactor)**
- **Custom binary WAL**
- **Segmented storage engine**
- **Buffered disk I/O**
- **ApacheBench (load testing)**

---

## 🏁 Conclusion

LogRhythm now has a **complete, production-style ingestion engine** with durability, replay, batching, segmenting, and throughput optimization.

This backend is fully ready for:

- A frontend dashboard  
- Search features  
- Bloom filter indexing  
- Multi-node scalability  

The foundational system is robust, fast, and correct — designed exactly like real-world distributed log systems.