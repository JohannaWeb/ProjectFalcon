# Project Falcon

Project Falcon is a sovereign real-time communication layer built on the AT Protocol and powered by a **Decentralized AI Inference Mesh**.

Where Discord owns your identity and Slack owns your data, Falcon owns neither. Your identity is a DID. Your data lives on the protocol. Your intelligence is distributed. No platform lock-in. No permission required. No centralized model censorship.

This is infrastructure, not a product. The client is a proof of concept. The protocol layer and the decentralized inference engine are the point.

---

## Why

Every collaboration platform you use today is a walled garden. Your Slack messages belong to Slack. Your Discord servers belong to Discord. Your identity is a username in someone else's database. Your AI assistants are black boxes controlled by single corporations.

The AT Protocol and Decentralized AI change that. Identity is portable. Data is portable. Intelligence is local and verifiable. Platforms become optional.

Falcon is built on that premise — a communication layer where the infrastructure and the intelligence are sovereign by design, not by promise.

---

## Architecture

The system operates through a multi-layered stack designed for high throughput and zero trust.

### Zero-Trust Gateway
Cryptographic DID/JWT verification occurs on every request. There is no session store and no centralized auth. Your identity is verified against the AT Protocol directly.

### Decentralized AI Mesh
Instead of routing prompts to a central API, Falcon utilizes a Peer-to-Peer Model Execution layer. This integrates directly with the SovereignTrustGraph to validate model weights and execution honesty across the network.

### Sovereign Integration Vessels (SIVs)
Real-time signal ingestion from developer ecosystems like GitHub, Vercel, and ATProto. Each SIV normalizes external events into the Falcon protocol layer.

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
Navigate to the backend directory and run the Spring Boot application.

```bash
cd backend
mvn spring-boot:run
