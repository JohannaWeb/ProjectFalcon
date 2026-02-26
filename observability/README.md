# Falcon Observability

> Prometheus + Grafana + Loki + Promtail

## Quick Start

```bash
# From project root
cd observability
docker-compose up -d
```

| Service    | URL                      | Credentials  |
|------------|--------------------------|--------------|
| Grafana    | http://localhost:3001    | admin/falcon |
| Prometheus | http://localhost:9090    | —            |
| Loki       | http://localhost:3100    | —            |

The **Project Falcon** dashboard loads automatically in Grafana.

---

## What's Included

**Grafana Dashboard** (`grafana/dashboards/falcon.json`)
- 🟢 Service UP/DOWN status for all 3 services
- 📈 Requests/sec + p50/p99 latency per service
- 🧠 JVM heap memory, thread count (virtual + platform), GC pauses
- 📋 Live log stream from all services, filterable by level/service/text

**Prometheus** (`prometheus/prometheus.yml`)  
Scrapes `/actuator/prometheus` on all three services every 15 seconds.
Services must be running on the host — `host.docker.internal` is used to bridge Docker → host.

**Loki + Promtail** (`loki/`, `promtail/`)  
Promtail ships logs from `logs/gateway/`, `logs/trust/`, `logs/siv/` to Loki.  
Log files are written there automatically when the services start (configured via `logging.file.path`).

---

## Service Ports

| Service       | Port | Metrics endpoint          |
|---------------|------|---------------------------|
| falcon-gateway| 8080 | /actuator/prometheus      |
| trust-service | 8081 | /actuator/prometheus      |
| siv-service   | 8082 | /actuator/prometheus      |

---

## Switching to Production

1. Replace `host.docker.internal` in `prometheus.yml` with actual hostnames
2. Update `promtail-config.yml` log paths to match your deployment's log locations
3. Set `GF_SECURITY_ADMIN_PASSWORD` to something other than `falcon`
4. Add persistent volume mounts for production longevity
