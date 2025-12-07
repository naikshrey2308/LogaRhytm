# Distributed Log Analyzer

A lightweight, single-node log analytics engine built using **Java + Spring WebFlux**, featuring high‑throughput ingestion, durable Write‑Ahead Logging (WAL), segment‑based storage, and Bloom‑filter–accelerated search.

This project showcases real systems‑design concepts used in log search engines like Loki, ClickHouse, and Elasticsearch—implemented from scratch in an interview‑ready, production‑inspired architecture.

---

## 🚀 Features

- Asynchronous WebFlux ingestion pipeline  
- WAL-based durability with crash recovery  
- Binary segment storage with timestamp & level encoding  
- Per‑segment Bloom Filters for fast skipping  
- Unified query API with limit, time range, level, and keyword search  
- Benchmarked ingestion & query performance  
- Minimal, extensible architecture ready for a UI  

---

## 🏗️ High‑Level System Architecture (Simplified)

```
React UI
   │
   ├──>  /ingest  ──> Sink ──> Ingestion Pipeline ──> WAL ──> Segment Storage
   │                                                       └──> Bloom Filters
   │
   └──>  /query   ────> Query Engine ──> Bloom Filters ──> Segment Scan ──> Results
```

---

## ⚙️ Technology Stack

| Component | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring WebFlux |
| Storage | Local filesystem (WAL + binary segments) |
| Search Acceleration | Bloom Filters |
| Concurrency Model | Reactive non‑blocking pipeline |
| Benchmark Tool | Apache Bench (ab) |

---

## 📊 Performance Metrics (Apache Bench)

### **Ingestion Throughput**
- **4075 logs/s**
- **P95 latency:** 34 ms

### **Query (Without Bloom Filters)**
- **206 RPS**
- **P98 latency:** 151 ms

### **Query (With Bloom Filters Enabled)**
- **230 RPS**
- **P98 latency:** 126 ms

Bloom filters reduce unnecessary segment scans and improve tail latency.

---

## 📁 Project Structure

```
backend/
  ├── core/
  │    ├── ingestion/        # Sink + ingestion pipeline
  │    ├── wal/              # WAL + checkpointing + replay
  │    ├── storage/          # Segment writer/reader
  │    └── bloom/            # Bloom filter engine
  ├── api/                   # REST endpoints
  └── model/                 # LogEntry record
```

---

## 📝 Notes

- System intentionally single-node for clarity.
- WAL ensures no data loss; checkpointing enables safe cleanup.
- Segments store logs efficiently using a compact binary format.
- Bloom filters drastically reduce read amplification.
- Architecture is extensible for:
  - sharding  
  - compaction  
  - indexing  
  - advanced UI  

---

## 📜 License

MIT License — free for personal and educational use.
