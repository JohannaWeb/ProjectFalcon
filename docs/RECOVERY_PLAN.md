# Recovery Plan — Project Falcon / Juntos

*Created: 2026-05-10*
*Companion to `docs/BRUTAL_ANALYSIS.md`. Concrete, dated, checkbox-tracked.*

---

## How to use this doc

- Tick items as you complete them: `- [x]`.
- Each phase has a **gate**: don't start the next phase until the gate is green.
- If an item slips, move it forward — don't delete it silently.
- "Out of scope" lists are as load-bearing as the in-scope lists. Defending scope is the work.

---

## Phase 0 — Stop the bleeding (Week 1)

**Goal:** stop adding surface area. Make the repo coherent before touching code.

### Reduce inflamed scope

- [ ] Decide the single product identity (recommended: **Juntos — LGBT decentralized community**) and write it in one sentence
- [ ] Make `juntos` the default branch on GitHub and Codeberg
- [ ] Archive `develop`, `yc-alpha`, and `main` branches (rename to `archive/<name>`, don't delete)
- [ ] Move all aspirational docs into `docs/archive/`: `MANIFESTO.md`, `PITCH.MD`, `ROADMAP.md` (rewrite later), `RESEARCH_NOTES.md`, `ARCHITECTURE.md` (rewrite later), `USER_JOURNEY.md`
- [ ] Keep at root: `README.md`, `FOUNDER_STORY.md`, `LICENSE`, `SECURITY.md`, `CONTRIBUTING.md` (add `.md` extension)
- [ ] Rewrite `README.md` to describe **only what runs today**, no future tense
- [ ] Audit every use of the word "Sovereign" — keep one, delete or rename the rest
- [ ] Delete `ProjectFalcon_arXiv_Package.zip` from the repo (binary artefact in git)
- [ ] Add a `docs/archive/README.md` explaining "these were earlier framings; current product is X"

### Repo hygiene

- [ ] Add `target/`, `dist/`, `node_modules/`, `out/` to `.gitignore` and confirm they're not tracked
- [ ] Run `git rm -r --cached target/` and equivalents for any tracked build output
- [ ] Run `git log --all -- .env.local` and any other `.env*`; rotate any secret found in history
- [ ] Add a pre-commit hook (or CI check) that fails on tracked `.class`, `.jar`, `.zip` files

### Un-break CI

- [ ] List every test currently skipped in CI and the commit that skipped it
- [ ] For each skipped test: fix it, delete it, or file an issue with a deadline — no fourth option
- [ ] Re-enable the test stage in CI workflow
- [ ] Confirm `mvn test` passes cleanly on a fresh checkout
- [ ] Confirm frontend `tsc -b` passes on a fresh checkout

**Gate to leave Phase 0:**
- A new visitor lands on the repo and can answer "what is this?" in under 30 seconds
- CI is green with tests actually running
- No build artefacts in git

---

## Phase 1 — Foundations: security, identity, federation (Weeks 2–4)

**Goal:** the things that ship in Phase 3 must rest on a real auth story, real federation, and zero known vulns. Build that floor now.

### Fix vulnerabilities

- [ ] Run `mvn dependency:tree` and `npm audit` on every module; record output in `docs/security/audit-2026-05.md`
- [ ] Add Dependabot or Renovate config for both Maven and npm
- [ ] Remove hardcoded Grafana credentials from `docker-compose.yml`; load from `.env` with no default
- [ ] Audit `TokenEncryptionService` — document the algorithm, key source, key rotation story; fix or rewrite if any of those are unclear
- [ ] Decide: Java 21 (matches Dockerfile) or Java 25 (matches pom). Make pom, Dockerfile, and CI agree
- [ ] Remove `--enable-preview` unless a specific preview feature is in use; preview flags in production are a footgun
- [ ] Add OWASP dependency-check Maven plugin, gate CI on high/critical CVEs
- [ ] Write `SECURITY.md` with: scope, contact, response SLA, supported versions
- [ ] Add rate limiting on `falcon-gateway` (Bucket4j or Spring rate limiter)
- [ ] Add CORS allowlist on the gateway — no `*` in production config
- [ ] Audit every `@Value` default that points at a localhost or test value; ensure prod fails closed

### Add OAuth / map OAuth → DID:PLC

- [ ] Read the AT Protocol OAuth spec end-to-end; link it in this doc
- [ ] Decide: implement AT Protocol OAuth (PAR + DPoP) directly, or use an existing client lib (`@atproto/oauth-client-node` for the JS side, evaluate JVM options)
- [ ] Implement the OAuth client metadata endpoint (`/.well-known/oauth-client-metadata.json`)
- [ ] Implement PKCE + PAR authorization request flow on the gateway
- [ ] Implement DPoP-bound token handling on the gateway
- [ ] On successful OAuth exchange: resolve the user's DID:PLC, cache the DID document, link the OAuth subject to the DID
- [ ] Replace any password-based atproto login (`com.atproto.server.createSession`) in the frontend with the OAuth flow
- [ ] Store OAuth tokens encrypted at rest; never log them
- [ ] Add automated test: full OAuth round-trip against a test PDS
- [ ] Document the flow in `docs/auth.md` with a sequence diagram

### Eurosky / European federation

- [ ] Confirm what "Eurosky" means for this project — write a one-paragraph definition in this doc and link the upstream effort
- [ ] Identify the Eurosky relay / PDS endpoints to integrate with
- [ ] Make the Jetstream / relay URL a config value (`falcon.atproto.relay-url`) — no hardcoded `bsky.network`
- [ ] Test ingestion against a Eurosky relay
- [ ] Make the PDS host configurable per user (so an EU-PDS user can sign in)
- [ ] Document Eurosky deployment posture in `docs/federation.md`: which relay, which PDS, which DID method
- [ ] If hosting in the EU is a goal: pick a region (Hetzner FSN/HEL, OVH GRA, Scaleway PAR), record the choice and reasoning
- [ ] Add a "data residency" section to the privacy/security docs

**Gate to leave Phase 1:**
- A user can log in via AT Protocol OAuth, end-to-end, against a Eurosky-side PDS
- `npm audit` and `mvn dependency-check` report zero high/critical findings
- One file states the canonical Java version; pom + Dockerfile + CI agree

---

## Phase 2 — Spring → Quarkus migration (Weeks 5–8)

**Goal:** smaller image, faster boot, native-compile path, less ceremony. Don't migrate just to migrate — migrate because the current Spring footprint is oversized for what the app does.

### Pre-migration

- [ ] Write a one-page rationale in `docs/migration-quarkus.md`: why Quarkus, what we expect to gain (image size, boot time, RSS), what we're prepared to lose
- [ ] Benchmark the current state: cold-start time, image size, RSS at idle, p95 latency on the auth filter — record numbers so the migration can be evaluated
- [ ] Decide JVM mode vs native (recommended: start JVM, plan native for later)
- [ ] Decide on the consolidation question first (see Phase 3): migrating four services is 4x the migration

### Migration order (smallest first, prove the pattern)

- [ ] **`trust-service`** — migrate first; smallest, no AT Protocol surface
  - [ ] Replace `spring-boot-starter-web` with `quarkus-resteasy-reactive`
  - [ ] Replace `spring-boot-starter-data-jpa` with `quarkus-hibernate-orm-panache`
  - [ ] Replace `@RestController` / `@Service` with JAX-RS + CDI `@ApplicationScoped`
  - [ ] Port repositories from Spring Data to Panache (or keep JPA repositories with `quarkus-hibernate-orm`)
  - [ ] Replace `application.yml` with `application.properties` (or keep yaml via `quarkus-config-yaml`)
  - [ ] Replace `spring-boot-starter-actuator` + Micrometer with `quarkus-micrometer-registry-prometheus` + `quarkus-smallrye-health`
  - [ ] Port tests from `@SpringBootTest` to `@QuarkusTest`
  - [ ] Verify Prometheus scrape, health endpoints, and DB connectivity in a docker-compose run
  - [ ] Update Dockerfile target to use Quarkus base image
- [ ] **`siv-service`** — migrate second; ports the WebSocket + AI patterns
  - [ ] Replace Spring `TextWebSocketHandler` (`JetstreamHandler`) with `quarkus-websockets-next`
  - [ ] Port `AiContextService` and `SovereignAgentService` to CDI beans
  - [ ] Port `FalconAiClient` HTTP calls to MicroProfile Rest Client or Quarkus REST client
  - [ ] Verify Jetstream ingestion still works against a real relay
  - [ ] Verify AI processing path end-to-end
- [ ] **`falcon-gateway`** — migrate third; the auth filter is the most sensitive
  - [ ] Replace Spring Cloud Gateway with Quarkus `quarkus-reactive-routes` or a hand-rolled proxy on Vert.x
  - [ ] Reimplement `AtprotoAuthFilter` as a Vert.x route handler — preserve ES256 / ES256K verification semantics exactly
  - [ ] Reimplement `GatedAccessFilter` semantics
  - [ ] Reimplement `DidResolver` (port the caching behaviour)
  - [ ] Add automated test: signed JWT in, gateway forwards with `X-Falcon-Viewer-DID` header set; bad signature → 401
- [ ] **`falcon-core`** — fold into whichever consumer needs it once the others are migrated; or keep as a shared library if Quarkus tooling supports it cleanly

### Post-migration

- [ ] Re-run the benchmark suite from "pre-migration"; record deltas in `docs/migration-quarkus.md`
- [ ] Build native images for one service (start with `trust-service`); record size + cold-start
- [ ] Update `Dockerfile` and `docker-compose.yml` for the new artefacts
- [ ] Decide whether to commit to native for production based on measured numbers, not vibes
- [ ] Delete Spring dependencies and Spring-specific config files from the repo

**Gate to leave Phase 2:**
- All migrated services pass their integration tests
- Measured improvement in at least two of: image size, cold-start, idle RSS
- Auth filter behaviour is byte-for-byte equivalent to the Spring version (same JWTs verify, same ones reject)

---

## Phase 3 — Consolidate the frontend and the architecture (parallel with Phase 2)

**Goal:** stop maintaining two of everything.

### One frontend

- [ ] Decide: **web-only PWA** (recommended for reach) or **Electron-only** (heavier, but matches the "premium desktop" pitch)
- [ ] If web-only: delete `electron-app/` after extracting anything unique (e.g. desktop notifications) into the web app
- [ ] If Electron-only: convert `electron-app/` to wrap the `falcon-web` build, delete `falcon-web/` as a standalone deploy target
- [ ] Pin a single React major version
- [ ] Pin a single `@atproto/api` version
- [ ] Move shared `lib/` and `components/` code into one location; no copy-paste
- [ ] Add a CI check that fails if `falcon-web/` and `electron-app/` ever both exist with components

### Reduce service count

- [ ] Decide: monolith, or 2-service split (gateway + everything-else). Recommended: monolith until 10x the current code
- [ ] Collapse `falcon-core`, `trust-service`, `siv-service` into one Quarkus app with internal modules
- [ ] Keep `falcon-gateway` separate **only if** it does real edge work (rate limiting, auth termination, multi-backend routing). Otherwise fold it in.
- [ ] Update `docker-compose.yml`, `k8s/`, and CI to match the new shape
- [ ] Delete the per-service `application.yml`/`.properties` files no longer in use

### Observability right-size

- [ ] Drop Loki + Promtail until log volume justifies them (stdout + `docker logs` is fine for now)
- [ ] Keep Prometheus + Grafana; trim dashboards to ones you actually look at
- [ ] Define three SLIs you'll watch (e.g. auth-filter p95, jetstream lag, AI call success rate); everything else is decoration

**Gate to leave Phase 3:**
- One frontend in the repo, one React version, one `@atproto/api` version
- One backend deploy artefact (or two: gateway + app)
- A new contributor can run the full stack with one command

---

## Phase 4 — Ship to real users (Week 9+)

**Goal:** the only metric that matters from here is "are queer people using it and coming back."

- [ ] Recruit 5 trans/queer beta users (your network, friends-of-friends, Bluesky DMs)
- [ ] Sit with each one for 30 minutes while they use it; record what breaks and what confuses
- [ ] Fix the top 3 friction points before recruiting the next 5
- [ ] Write a short "why Juntos exists" post; publish on Bluesky and one other surface
- [ ] Define one retention metric (e.g. "users who post in week 2 after signup") and instrument it
- [ ] Set a 90-day target for that metric; review monthly

---

## Out of scope (defend this list)

These appear in earlier docs but are not in this plan. Do not work on them until Phase 4 ships:

- Distributed AI inference mesh
- On-chain trust attestation (EAS / web3j)
- Mobile native clients
- Slack/Discord enterprise bridges
- Probabilistic trust propagation models
- Cryptographic signing of `AiFact` (unless you commit to it as the AI agent's identity story — see Phase 1 decision)
- arXiv white paper updates
- The "Sovereign Successor to Slack and Discord" framing

If you find yourself working on any of these in the next 60 days, stop and re-read this section.

---

## Tracking

- Update this file weekly. Tick what's done. Move what slipped.
- One Friday-afternoon review per week is enough.
- If a phase blows past 2x its estimate, the issue is scope, not effort — cut something.
