# Project Falcon

**Project Falcon** is a Discord-style desktop client built on the **Bluesky AT Protocol**, designed for real communities, real identity, and open infrastructure.

Instead of relying on a closed platform, Falcon runs on decentralized identity and open protocols — enabling communities that users truly own.

---

## Vision

Modern chat platforms are powerful but centralized.

Falcon explores a different path:

- Identity comes from **AT Protocol**
- Communities are not locked to a single vendor
- Servers and channels are modeled through open lexicons
- The client remains familiar (Discord-like UX), but the foundation is decentralized

This project is an early step toward **open, community-owned communication infrastructure**.

---

## What Falcon does

- Discord-like desktop experience
- Servers and channels
- Message-first community interaction
- AT Protocol identity integration
- Custom AT lexicons for Falcon entities (`app.falcon.*`)

---

## Architecture

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

## Current status

Early prototype / architecture phase.

Working on:

- exposing Falcon APIs via AT lexicons
- modeling server + channel structures
- identity flow
- messaging foundations

---

## Roadmap

### Phase 1 — Core foundations
- [x] AT lexicon definitions for servers/channels/messages
- [x] Authentication flow
- [x] Basic server & channel management
- [x] Desktop client navigation

### Phase 2 — Real-time messaging
- [x] WebSocket layer
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

## Why this exists

Falcon is not just another chat clone.

It explores:

- what a **Discord-like UX** looks like on open identity
- how **AT Protocol** can support communities, not just social feeds
- how decentralized identity + structured messaging can coexist

---

## Project goals

- Learn and experiment with AT Protocol capabilities
- Build a reference implementation for community apps on Bluesky infra
- Explore scalable real-time messaging architecture
- Prototype an open alternative to centralized chat ecosystems

---

## Contributing

This project is in early development.

Contributions, feedback, and architectural discussions are welcome.

Areas especially useful:

- AT Protocol modeling
- messaging architecture
- Electron performance
- Spring real-time infra
- UI/UX

---

##  Long-term direction

Falcon could evolve toward:

- community-owned chat networks
- federated servers
- protocol-level messaging standards
- open developer ecosystem around Falcon lexicons

---

## Author

**Johanna**  
Building open infrastructure experiments around identity, messaging, and community systems.

## Manifesto

The Falcon Manifesto: Sovereign Connection

We Believe in Data Sovereignty Communication is a human right, not a corporate asset. By building on the AT Protocol, ProjectFalcon ensures that users—not platforms—own their identities, their relationships, and their data. We aren't building a walled garden; we are building a gate to the open web.

UX is the Ultimate Feature Decentralization has failed in the past because it was too difficult to use. ProjectFalcon exists to prove that "Sovereign" can also be "Seamless". We prioritize high-fidelity, real-time UX (Electron/Spring Boot) because beauty and speed are what make a platform feel like home.

Safety by Design, Not as an Afterthought As a founder from a marginalized background, I know that "free speech" without safety is just a vacuum for harassment. Falcon is built with the lived experience that moderation tools must be as robust as the protocol itself. We are building the first decentralized "safe space".

The Bridge to the Future We aren't just building a chat app; we are building the Communication Layer for the decentralized social era. We leverage the existing momentum of the ATmosphere to give users immediate value while paving the way for a world where "logging in" means bringing your own soul with you.

Iteration Over Perfection We ship while we’re scared. We code through the "nothing" phase. We are building a million-dollar reality, one commit at a time.
