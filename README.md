## Project Falcon

**Project Falcon** is a **Discord‑style desktop client** that runs on the **Bluesky AT Protocol** instead of Discord’s closed platform.

- **Desktop app**: Electron + TypeScript + React + Vite  
- **Backend**: Java 25 + Spring Boot 4 + WebSockets + H2  
- **Protocol**: Bluesky / AT Protocol, with custom **AT Lexicons** for Falcon’s servers and channels (`app.falcon.*`)

Falcon gives you Discord‑like servers, channels, and live chat, but identity and social graph come from your Bluesky account.

---

## What you get

### AT Protocol features (Bluesky)
- **Sign in with Bluesky**: handle + app password (no separate account system).
- **Home feed**: timeline with like, repost, reply, and thread view.
- **Compose & post**: text posts (+ optional image) and replies.
- **Profiles**: view profiles, follow / unfollow.
- **Notifications**: list, inspect, and mark as read.
- **Search**: search users by handle or display name.
- **Explore**: suggested feeds and custom feed generators.

### Discord‑style servers (Falcon backend)
- **Servers sidebar**: Home + your servers from the backend + “+” to create a new server.
- **Channels per server**: click a server → middle column shows its text channels (`#general`, etc.) and **Create Channel**.
- **Channel view**: click a channel → message list + input area, behaves like a minimal Discord text channel.
- **Invite by handle**: in a server header, click **Invite**, type a Bluesky handle, backend resolves handle → DID via AT Protocol and adds them as a member.

### UX niceties
- **Dark / light theme** with header toggle; keyboard shortcut **Ctrl+Shift+L**.
- **System tray** (when `electron-app/assets/tray-icon.png` is present): quick Show / Quit from the OS tray.

---

## Architecture at a glance

- **Electron renderer ↔ Bluesky (PDS)** using `@atproto/api` for all standard Bluesky features.
- **Electron renderer ↔ Falcon backend** using **AT Lexicon‑defined XRPC** (`/xrpc/app.falcon.*`) for servers, channels, and messages.
- **Falcon backend** (Spring Boot):
  - Authenticates requests by validating the AT access JWT with `com.atproto.server.getSession`.
  - Persists servers / channels / messages / members in an H2 file DB.
  - Pushes realtime channel messages over WebSockets to the Electron client.

---

## Tech stack

| **Layer** | **Tech** |
|----------|----------|
| Desktop app | Electron + TypeScript + React + Vite |
| AT Protocol client (frontend) | `@atproto/api` (official Bluesky TS SDK) |
| Backend | Java 25 + Spring Boot 4 + Spring MVC + JPA + H2 |
| AT Protocol from Java | Raw XRPC over HTTP via `XrpcClient` (no official Java SDK) |
| Falcon API surface | AT Lexicons (`app.falcon.*`) + XRPC (`/xrpc/app.falcon.…`) |

---

## Quick start

### 1. Start the backend (Java)

```bash
cd backend
mvn spring-boot:run
```

- Base URL: `http://localhost:8080`

**Health / auth (REST):**
- `GET /api/health` — health check
- `GET /api/atproto/validate` — validate access JWT

**Falcon servers & channels (XRPC, AT Lexicons):**  
All require `Authorization: Bearer <AT access JWT>`.

| **Method** | **NSID**                         | **Params / body**                                              |
|-----------|-----------------------------------|-----------------------------------------------------------------|
| GET       | `app.falcon.server.list`         | —                                                               |
| GET       | `app.falcon.server.get`          | `serverId` (query)                                             |
| POST      | `app.falcon.server.create`       | body: `{ "name": "..." }`                                      |
| POST      | `app.falcon.server.invite`       | `serverId` (query), body: `{ "handle": "user.bsky.social" }`  |
| GET       | `app.falcon.channel.list`        | `serverId` (query)                                             |
| POST      | `app.falcon.channel.create`      | `serverId` (query), body: `{ "name": "..." }`                  |
| GET       | `app.falcon.channel.getMessages` | `channelId`, `limit` (query)                                   |
| POST      | `app.falcon.channel.postMessage` | `channelId` (query), body: `{ "content": "..." }`              |

Example:  
`GET /xrpc/app.falcon.server.list` with a valid Bearer token returns a JSON array of servers (see `lexicons/app.falcon.defs.json` for shapes).

Legacy REST under `/api/servers` and `/api/channels` still exists, but the Electron app now talks to the backend via **XRPC + Lexicons**.

### 2. Run the Electron app

```bash
cd electron-app
npm install
npm run dev          # Vite dev server only (http://localhost:5173)
npm run electron:dev # Vite + Electron window
```

For a packaged build:

```bash
npm run electron:build
```

### 3. Sign in with Bluesky

- Go to **Bluesky → Settings → App passwords** and generate an app password.
- In Falcon’s login screen, use:
  - **Handle**: your Bluesky handle (for example `alice.bsky.social`)
  - **Password**: the **app password** (not your main account password)

The Electron app talks directly to **`bsky.social`** via AT Protocol for feeds, profiles, etc.  
For **servers / channels / messages**, it calls the Falcon backend with your AT access JWT as a Bearer token.

---

## Project layout

```text
ATDISCORD/
├── README.md
├── lexicons/              # AT Lexicons (app.falcon.*) — queries, procedures, shared defs
├── electron-app/
│   ├── main/              # Electron main process (window, tray)
│   ├── preload/
│   ├── renderer/src/
│   │   ├── components/    # Login, Layout, FeedView, PostCard, ThreadView,
│   │   │                  # PostComposer, ProfileView, NotificationsView,
│   │   │                  # SearchView, ExploreView, ServersView, …
│   │   ├── contexts/      # ThemeContext
│   │   ├── hooks/         # useAtpSession
│   │   └── lib/           # atp.ts (AT client), backendApi.ts (Falcon XRPC client)
│   ├── assets/            # optional: tray-icon.png for system tray
│   └── package.json
└── backend/
    ├── src/main/java/app/falcon/
    │   ├── api/           # AuthController, ServerController, ChannelController, XrpcFalconController
    │   ├── atproto/       # XrpcClient (raw XRPC HTTP client)
    │   ├── domain/        # Server, Channel, Message, Member
    │   ├── repository/
    │   └── BackendApplication.java
    └── src/main/resources/application.yml  # H2 DB, atproto.service
```

---

## Config & environment

- **Electron renderer**
  - AT service base URL: `renderer/src/lib/atp.ts`
  - Falcon backend base URL: `renderer/src/lib/backendApi.ts` (default `http://localhost:8080`)
- **Backend (`application.yml`)**
  - `server.port` — HTTP port for the Spring app (default `8080`)
  - `atproto.service` — AT Protocol service, defaults to `https://bsky.social`
  - H2 DB file at `./data/falcon`

---

## Lexicons (app.falcon.*)

Falcon’s servers and channels are modeled as **AT Lexicons** in the `app.falcon` namespace. Lexicon JSON lives in `lexicons/`:

- **`app.falcon.defs.json`** — shared types:
  - `serverView`, `channelRef`, `channelView`, `messageView`, `inviteResult`
- **Server API:**
  - `app.falcon.server.list`, `app.falcon.server.get`
  - `app.falcon.server.create`, `app.falcon.server.invite`
- **Channel & message API:**
  - `app.falcon.channel.list`, `app.falcon.channel.create`
  - `app.falcon.channel.getMessages`, `app.falcon.channel.postMessage`

The backend exposes these via standard AT XRPC:

- `GET /xrpc/app.falcon.server.list`
- `POST /xrpc/app.falcon.channel.postMessage`
- …etc.

For production deployments, you should publish these lexicons under a domain you control and wire up DNS `_lexicon` TXT records so other services can resolve them (see the official Lexicon spec).

---

## Development notes

- **Backend**: standard Spring Boot app — you can run it from your IDE or via `mvn spring-boot:run`.
- **DB**: H2 file DB by default; safe to delete `./data/falcon` during local development.
- **Auth**: every Falcon XRPC call is guarded by `AtprotoAuthFilter`, which:
  - Extracts `Authorization: Bearer <token>`
  - Calls `com.atproto.server.getSession` via `XrpcClient`
  - Injects `auth.userDid` and `auth.userHandle` into the request
- **Realtime**: `RealtimeBroker` + `/ws` WebSocket endpoint; `ChannelView` subscribes to new messages per‑channel.

---

## References

- [AT Protocol](https://atproto.com/)
- [Lexicon](https://atproto.com/specs/lexicon) — schema language for records & XRPC
- [Bluesky API (XRPC)](https://docs.bsky.app/docs/api/at-protocol-xrpc-api)
- [@atproto/api (npm)](https://www.npmjs.com/package/@atproto/api)

