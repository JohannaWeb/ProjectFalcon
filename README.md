# Project Falcon

Project Falcon is a sovereign real-time communication layer built on the AT Protocol.

Where Discord owns your identity and Slack owns your data, Falcon owns neither.
Your identity is a DID. Your data lives on the protocol. No platform lock-in. 
No permission required.

This is infrastructure, not a product. The client is a proof of concept.
The protocol layer is the point.

---

## Why

Every collaboration platform you use today is a walled garden.

Your Slack messages belong to Slack. Your Discord servers belong to Discord.
Your identity is a username in someone else's database.

The AT Protocol changes that. Identity is portable. Data is portable.
Platforms become optional.

Falcon is built on that premise — a communication layer where the infrastructure
is sovereign by design, not by promise.

---

## Architecture
```
┌─────────────────────────────────────────────┐
│              Electron Client                │
│         TypeScript + React + Vite           │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│           Zero-Trust Gateway                │
│     Cryptographic DID/JWT Verification      │
│         No centralized user store           │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│         Java 25 + Spring Boot 4             │
│      Project Loom — Virtual Threads         │
│   Parallel SIV fetching + async recording   │
└───────────┬───────────────────┬─────────────┘
            │                   │
┌───────────▼──────┐  ┌─────────▼───────────────┐
│  AT Protocol     │  │  Sovereign Integration  │
│  Firehose        │  │  Vessels (SIVs)         │
│  Jetstream       │  │  GitHub · Vercel · ATP  │
└──────────────────┘  └─────────────────────────┘
```

### Core Components

**Zero-Trust Gateway**
Cryptographic DID/JWT verification on every request. No session store.
No centralized auth. Your identity is verified against the AT Protocol directly.

**Sovereign Integration Vessels (SIVs)**
Real-time signal ingestion from developer ecosystems — GitHub, Vercel, ATProto.
Each SIV normalizes external events into the Falcon protocol layer.

**SovereignTrustGraph**
Decentralized peer-to-peer trust scoring. Trust is computed, not assigned.
No platform decides who you can talk to.

**Jetstream Ingestion**
Real-time connection to the AT Protocol firehose.
Sub-60ms event latency. Built on Project Loom virtual threads.

---

## Stack

| Layer | Tech |
|---|---|
| Desktop client | Electron + TypeScript + React + Vite |
| AT Protocol client | @atproto/api (official TS SDK) |
| Backend | Java 25 + Spring Boot 4 + Project Loom |
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

API runs at `http://localhost:8080`

| Endpoint | Description |
|---|---|
| `GET /api/health` | Health check |
| `GET /api/atproto/validate` | Validate AT Protocol JWT |
| `GET /api/servers` | List servers |
| `POST /api/servers` | Create server |
| `POST /api/servers/{id}/invite` | Invite by Bluesky handle |
| `GET /api/channels/{id}/messages` | Get messages |
| `POST /api/channels/{id}/messages` | Post message |

All endpoints require `Authorization: Bearer <AT access JWT>`.

### 2. Client
```bash
cd electron-app
npm install
npm run electron:dev    # Vite + Electron
npm run electron:build  # Packaged build
```

### 3. Sign In

Use your Bluesky handle and an app password from **Bluesky → Settings → App passwords**.

---

## Project Structure
```
ProjectFalcon/
├── backend/
│   └── src/main/java/app/falcon/
│       ├── api/          # Controllers
│       ├── atproto/      # XRPC client + DID resolver
│       ├── domain/       # Server, Channel, Message, Member
│       ├── gateway/      # Zero-Trust JWT/DID verification
│       ├── siv/          # Sovereign Integration Vessels
│       └── trust/        # SovereignTrustGraph
├── electron-app/
│   └── renderer/src/
│       ├── components/   # UI components
│       ├── contexts/     # ThemeContext
│       ├── hooks/        # useAtpSession
│       └── lib/          # atp.ts, backendApi.ts
└── .github/workflows/    # CI/CD
```

---

## Status

Active development. Shipping daily.

- [x] AT Protocol authentication + DID verification
- [x] Zero-Trust Gateway
- [x] Jetstream firehose ingestion
- [x] Sovereign Integration Vessels (GitHub, Vercel, ATProto)
- [x] SovereignTrustGraph
- [x] Microservices monorepo
- [x] Project Loom virtual threads
- [ ] Federation between Falcon instances
- [ ] Mobile client
- [ ] Public beta

---

## References

- [AT Protocol](https://atproto.com/)
- [Bluesky API](https://docs.bsky.app/docs/api/at-protocol-xrpc-api)
- [Project Loom](https://openjdk.org/projects/loom/)
- [DID:PLC](https://web.plc.directory/)

---

## License

GNU General Public License v3.0

---

*Built in Porto. Shipping daily.*
