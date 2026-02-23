# Project Falcon

Project Falcon is a **Discord-like** desktop app built on the **Bluesky AT Protocol**. Same client stack as Discord: **Electron + TypeScript**. Backend: **Java (Spring Boot)**.

## What it does

### AT Protocol (Bluesky)
- **Sign in** with your Bluesky handle and app password.
- **Home feed**: timeline with like, repost, reply, and thread view.
- **Compose & post**: create posts (text + optional image) and replies.
- **Profile**: view profiles, follow/unfollow.
- **Notifications**: list and mark as read.
- **Search**: search users by handle or name.
- **Explore**: suggested feeds and custom feed generators.

### Discord-style Servers (Java backend)
- **Left sidebar**: Home icon + your servers (from backend) + “+” to create a server.
- **Server view**: Click a server → middle column shows its text channels (# general, etc.) and “Create Channel”.
- **Channel view**: Click a channel → main area shows messages and a message input (like Discord).
- **Invite**: When viewing a server, use the “Invite” button in the header; enter a Bluesky handle to add them (backend resolves handle → DID via AT Protocol).

### UX
- **Dark / light theme** with toggle in header; shortcut **Ctrl+Shift+L**.
- **System tray** (when `electron-app/assets/tray-icon.png` is present): Show / Quit.

## Stack

| Layer   | Tech |
|--------|------|
| Desktop app | **Electron** + **TypeScript** + **React** + **Vite** |
| AT Protocol client | **@atproto/api** (Bluesky official TS SDK) |
| Backend | **Java 21** + **Spring Boot 3** + **WebFlux** + **JPA** + **H2** |
| AT Protocol from Java | **XRPC over HTTP** (no official Java SDK) |

## Quick start

### 1. Backend (Java)

```bash
cd backend
mvn spring-boot:run
```

API: `http://localhost:8080`
- `GET /api/health` — health check
- `GET /api/atproto/validate` — validate access JWT
- `GET /api/servers` — list servers (header: `X-User-Did`)
- `GET /api/servers/{id}` — get one server with channels
- `POST /api/servers` — create server (body: `{ "name": "..." }`)
- `POST /api/servers/{id}/invite` — invite by Bluesky handle (body: `{ "handle": "user.bsky.social" }`; backend resolves handle to DID via AT Protocol)
- `GET /api/channels/server/{id}` — list channels
- `POST /api/channels/server/{id}` — create channel
- `GET /api/channels/{id}/messages` — get messages
- `POST /api/channels/{id}/messages` — post message (body: `{ "content": "..." }`)

All channel/server endpoints require `X-User-Did` (and optionally `X-User-Handle`).

### 2. Electron app

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

### 3. Sign in

Use your **Bluesky handle** and an **app password** from **Bluesky → Settings → App passwords**.

The app talks to **bsky.social** via the AT Protocol. For **Servers** (Discord-style channels), the Java backend must be running; the app sends your DID and handle in headers.

## Project layout

```
ATDISCORD/
├── README.md
├── electron-app/
│   ├── main/               # Electron main process (tray optional)
│   ├── preload/
│   ├── renderer/src/
│   │   ├── components/     # Login, Layout, FeedView, PostCard, ThreadView,
│   │   │                   # PostComposer, ProfileView, NotificationsView,
│   │   │                   # SearchView, ExploreView, ServersView, …
│   │   ├── contexts/       # ThemeContext
│   │   ├── hooks/          # useAtpSession
│   │   └── lib/            # atp.ts, backendApi.ts
│   ├── assets/             # optional: tray-icon.png for system tray
│   └── package.json
└── backend/
    ├── src/main/java/app/atdiscord/
    │   ├── api/            # AuthController, ServerController, ChannelController
    │   ├── atproto/        # XrpcClient
    │   ├── domain/         # Server, Channel, Message, Member
    │   ├── repository/
    │   └── BackendApplication.java
    └── src/main/resources/application.yml  # H2 DB, atproto.service
```

## Config

- **Electron**: AT Protocol service in `renderer/src/lib/atp.ts`; backend URL in `renderer/src/lib/backendApi.ts` (default `http://localhost:8080`).
- **Backend**: `application.yml` — `server.port`, `atproto.service`, H2 file DB at `./data/atdiscord`.

## References

- [AT Protocol](https://atproto.com/)
- [Bluesky API (XRPC)](https://docs.bsky.app/docs/api/at-protocol-xrpc-api)
- [@atproto/api (npm)](https://www.npmjs.com/package/@atproto/api)
