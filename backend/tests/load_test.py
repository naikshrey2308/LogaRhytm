import requests
import time
import threading
import json
import uuid

URL = "http://localhost:8080/ingest"

# number of threads hitting the ingestion endpoint
THREADS = 8

# how long to run the test (seconds)
TEST_DURATION = 10

# shared counter
count = 0
count_lock = threading.Lock()

payload_template = {
    "timestamp": 0,
    "level": "INFO",
    "message": ""
}

def worker():
    global count
    end_time = time.time() + TEST_DURATION

    while time.time() < end_time:
        payload = payload_template.copy()
        payload["timestamp"] = int(time.time() * 1000)
        payload["message"] = "baseline-test-" + str(uuid.uuid4())  # unique msg

        # fire-and-forget POST
        try:
            requests.post(URL, json=payload, timeout=0.3)
        except:
            continue  # ignore timeouts during high load

        # thread-safe increment
        with count_lock:
            count += 1

threads = []
start = time.time()

# spawn workers
for _ in range(THREADS):
    t = threading.Thread(target=worker)
    t.start()
    threads.append(t)

for t in threads:
    t.join()

elapsed = time.time() - start
eps = count / elapsed

print("\n========== BASELINE METRICS ==========")
print(f"Threads: {THREADS}")
print(f"Test duration: {TEST_DURATION:.1f}s")
print(f"Total logs sent: {count}")
print(f"Ingestion throughput: {eps:.2f} logs/sec")
print("=======================================\n")