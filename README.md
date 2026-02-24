# Project Falcon

**Project Falcon** is a Discord-style desktop client built on the **Bluesky AT Protocol**, designed for real communities, real identity, and open infrastructure.

Instead of relying on a closed platform, Falcon runs on decentralized identity and open protocols — enabling communities that users truly own.

---

## ✨ Vision

Modern chat platforms are powerful but centralized.

Falcon explores a different path:

- Identity comes from **AT Protocol**
- Communities are not locked to a single vendor
- Servers and channels are modeled through open lexicons
- The client remains familiar (Discord-like UX), but the foundation is decentralized

This project is an early step toward **open, community-owned communication infrastructure**.

---

## 🧠 What Falcon does

- Discord-like desktop experience
- Servers and channels
- Message-first community interaction
- AT Protocol identity integration
- Custom AT lexicons for Falcon entities (`app.falcon.*`)

---

## 🏗 Architecture

### Desktop client
- Electron
- React
- TypeScript
- Vite

### Backend
- Java 25
- Spring Boot
- REST APIs (WebSockets planned)
- H2 (dev) → future production DB

### Protocol layer
- Bluesky AT Protocol
- Falcon lexicons for:
  - servers
  - channels
  - message structures

---

## 🚀 Current status

Early prototype / architecture phase.

Working on:

- exposing Falcon APIs via AT lexicons
- modeling server + channel structures
- identity flow
- messaging foundations

---

## 🗺 Roadmap

### Phase 1 — Core foundations
- [ ] AT lexicon definitions for servers/channels/messages
- [ ] Authentication flow
- [ ] Basic server & channel management
- [ ] Desktop client navigation

### Phase 2 — Real-time messaging
- [ ] WebSocket layer
- [ ] Presence system
- [ ] Message streaming
- [ ] Notifications

### Phase 3 — Community layer
- [ ] Roles & permissions
- [ ] Moderation tools
- [ ] Attachments/media
- [ ] Search

### Phase 4 — Scale & infra
- [ ] Horizontal backend scaling
- [ ] Redis/pub-sub for fanout
- [ ] Event streaming
- [ ] Production deployment model

---

## 💡 Why this exists

Falcon is not just another chat clone.

It explores:

- what a **Discord-like UX** looks like on open identity
- how **AT Protocol** can support communities, not just social feeds
- how decentralized identity + structured messaging can coexist

---

## 🧪 Project goals

- Learn and experiment with AT Protocol capabilities
- Build a reference implementation for community apps on Bluesky infra
- Explore scalable real-time messaging architecture
- Prototype an open alternative to centralized chat ecosystems

---

## 🤝 Contributing

This project is in early development.

Contributions, feedback, and architectural discussions are welcome.

Areas especially useful:

- AT Protocol modeling
- messaging architecture
- Electron performance
- Spring real-time infra
- UI/UX

---

## 📌 Long-term direction

Falcon could evolve toward:

- community-owned chat networks
- federated servers
- protocol-level messaging standards
- open developer ecosystem around Falcon lexicons

---

## Author

**Johanna**  
Building open infrastructure experiments around identity, messaging, and community systems.