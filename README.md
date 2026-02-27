# Project Falcon

**Sovereign real-time communication infrastructure built on the AT Protocol and a Decentralized AI Inference Mesh.**

Where Discord owns your identity and Slack owns your data, Falcon owns neither. Your identity is a DID. Your data lives on the protocol. Your intelligence is distributed. No platform lock-in. No permission required. No centralized model censorship.

> This is infrastructure, not a product. The client is a proof of concept. The protocol layer and the decentralized inference engine are the point.

---

## Technical Nerding
 CI: 21s avg
 Time to feedback: <30s

## Why

Every collaboration platform you use today is a walled garden. Your Slack messages belong to Slack. Your Discord servers belong to Discord. Your identity is a username in someone else's database. Your AI assistants are black boxes controlled by single corporations.

The AT Protocol and decentralized AI change that. Identity is portable. Data is portable. Intelligence is local and verifiable. Platforms become optional.

Falcon is built on that premise — a communication layer where the infrastructure and the intelligence are sovereign by design, not by promise.

---

## Architecture

A multi-layered stack designed for high throughput and zero trust.

### Zero-Trust Gateway
Cryptographic DID/JWT verification on every request. No session store. No centralized auth. Identity is verified against the AT Protocol directly.

### Decentralized AI Mesh
Instead of routing prompts to a central API, Falcon uses a Peer-to-Peer Model Execution layer — integrated with the SovereignTrustGraph to validate model weights and execution honesty across the network.

### Sovereign Integration Vessels (SIVs)
Real-time signal ingestion from developer ecosystems (GitHub, Vercel, ATProto). Each SIV normalizes external events into the Falcon protocol layer.

### SovereignTrustGraph
Decentralized peer-to-peer trust scoring. Trust is computed, not assigned. No platform decides who you can talk to.

### Jetstream Ingestion
Real-time connection to the AT Protocol firehose. Sub-60ms event latency. Built on Project Loom virtual threads for massive concurrency.

---

## Stack

| Layer | Tech |
|---|---|
| Desktop client | Electron + TypeScript + React + Vite |
| AT Protocol client | @atproto/api (official TS SDK) |
| Backend | Java 25 + Spring Boot 4 + Project Loom |
| Intelligence | Decentralized AI Mesh (P2P Model Execution) |
| Identity | DID:PLC / DID:Web + JWT verification |
| Real-time | AT Protocol Jetstream firehose |
| Observability | Prometheus + Grafana + Promtail |
| Infrastructure | Docker + Kubernetes |

---

## Quick Start

### 1. Backend

```bash
cd backend
mvn spring-boot:run
```

API runs at `http://localhost:8080`. All endpoints require `Authorization: Bearer <AT access JWT>`.

### 2. Client

```bash
cd electron-app
npm install
npm run electron:dev
```

### 3. Sign In

Use your Bluesky handle and an app password from your Bluesky settings.

---

## Project Structure

```
backend/src/main/java/app/falcon/
├── api/          # REST endpoint controllers
├── atproto/      # XRPC client and DID resolver
├── ai/           # P2P AI Mesh and inference orchestration
├── gateway/      # Zero-Trust JWT/DID verification
├── siv/          # Sovereign Integration Vessels
└── trust/        # SovereignTrustGraph implementation

electron-app/renderer/src/
├── components/   # UI components and AI interface
├── hooks/        # useAtpSession and real-time listeners
└── lib/          # AT Protocol and backend API utilities
```

---

## Status

Active development. Shipping daily from Porto.

- [x] AT Protocol authentication + DID verification
- [x] Zero-Trust Gateway
- [x] Jetstream firehose ingestion
- [x] Sovereign Integration Vessels (GitHub, Vercel, ATProto)
- [x] SovereignTrustGraph
- [x] Decentralized AI Mesh integration
- [x] Project Loom virtual threads
- [ ] Federation between Falcon instances
- [ ] Mobile client
- [ ] Public beta

---

## License

GNU General Public License v3.0

---

*Built in Porto. Shipping daily.*
