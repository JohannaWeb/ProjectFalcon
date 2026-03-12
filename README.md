# [Falcon](https://github.com/JohannaWeb/ProjectFalcon)

[![CI](https://img.shields.io/github/actions/workflow/status/JohannaWeb/ProjectFalcon/ci.yml?branch=main&label=CI&logo=github)](https://github.com/JohannaWeb/ProjectFalcon/actions)
[![Docker Hub](https://img.shields.io/badge/docker-johannaweb%2Ffalcon-blue?logo=docker)](https://hub.docker.com/r/johannaweb/falcon/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/JohannaWeb/ProjectFalcon?logo=github)](https://github.com/JohannaWeb/ProjectFalcon/stargazers)
[![Java 25](https://img.shields.io/badge/java-25-orange?logo=java)](https://www.oracle.com/java/)
[![Spring Boot 4](https://img.shields.io/badge/spring%20boot-4.0-green?logo=spring)](https://spring.io/projects/spring-boot)

**A distributed microservice platform for human-AI collaboration on AT Protocol.**

Falcon, a decentralized systems platform, is a cryptographic trust and identity system for building applications with human-AI collaboration. It collects relationships from configured identity sources at given intervals, evaluates trust expressions, verifies cryptographic proofs, and can trigger alerts when adversarial conditions are observed.

The features that distinguish Falcon from other decentralized platforms are:

* A **graph-based trust model** (transitive relationships between DIDs verified cryptographically)
* **Trust evaluation as a first-class operation** (compute trust scores relative to any observer via PromQL-like queries)
* **Zero-trust architecture** (no implicit trust; all relationships cryptographically signed)
* **Native AT Protocol support** (Bluesky Communities & DMs with secp256k1 ES256K verification)
* **Human-AI collaboration primitives** (agents with cryptographic identity and approval workflows)
* **Autonomous microservices** (no dependency on centralized orchestration; services operate independently)
* **Multiple deployment modes** (Docker Compose, Railway, Vercel, Kubernetes)
* **Production observability** (Prometheus metrics, Grafana dashboards, centralized logging)

## Architecture overview

<img width="885" height="631" alt="image" src="https://github.com/user-attachments/assets/a0419528-408e-4c66-be48-f54c2003c4a5" />

## Install

There are various ways of installing Falcon.

### Precompiled binaries

Precompiled binaries for released versions are available in the [*releases*](https://github.com/JohannaWeb/ProjectFalcon/releases) section on GitHub. Using the latest production release binary is the recommended way of installing Falcon. See the [Installing](https://falcon.bigmoat.io/docs/install/) chapter in the documentation for all the details.

### Docker images

Docker images are available on [Docker Hub](https://hub.docker.com/r/johannaweb/falcon).

You can launch a Falcon container with the observability stack for trying it out with

```
docker-compose up -d
```

Falcon will now be reachable at `http://localhost:8080/` (API) and `http://localhost:5173/` (web UI).

### Building from source

To build Falcon from source code, you need:

* **Java 25**: Version specified in `pom.xml` or greater.
* **Node.js 20+**: Version specified in `falcon-web/package.json` or greater.
* **npm**: Version 10 or greater (check with `npm --version`).
* **Maven 3.9+**: For Java build tooling.

Start by cloning the repository:

```
git clone https://github.com/JohannaWeb/ProjectFalcon.git
cd ProjectFalcon
```

You can use Maven to build and install the backend:

```
cd falcon-alpha
mvn clean package -DskipTests
java -jar target/falcon-core-*.jar
```

The backend will be reachable at `http://localhost:8080/`.

To build the frontend in another terminal:

```
cd falcon-web
npm install
npm run dev
```

The frontend will now be reachable at `http://localhost:5173/`.

However, when using `mvn package` to build Falcon, the backend will expect configuration files to be present. An example configuration file can be found [here](./falcon-alpha/src/main/resources/application.yml).

You can also build the full stack using Docker Compose, which will compile in all assets so that services can be run from anywhere:

```
docker-compose up -d
```

The Docker Compose file provides several services:

* **falcon-core**: The main API and AT Protocol bridge
* **falcon-gateway**: Request routing and authentication
* **trust-service**: Transitive trust computation and Sybil detection
* **siv-service**: Semantic intent verification
* **agent-mesh**: AI agent lifecycle management
* **postgres**: State persistence
* **prometheus**: Metrics collection
* **grafana**: Dashboards and visualization
* **loki**: Centralized logging
* **promtail**: Log shipping

### Building from source (advanced)

To build only specific microservices, you can use Maven profiles:

```
cd falcon-alpha
mvn clean package -P core,gateway,trust
```

Available profiles:

* `core` — Build falcon-core (AT Protocol bridge)
* `gateway` — Build falcon-gateway (API gateway)
* `trust` — Build trust-service (trust computation)
* `siv` — Build siv-service (semantic intent)
* `agent` — Build agent-mesh (AI agents)

If you add out-of-tree services, additional steps might be needed to adjust the POM and Maven configuration. As always, be extra careful when loading third-party code.

### Building the Docker image

You can build a docker image locally with the following commands:

```
docker build -t falcon-core:latest -f falcon-alpha/Dockerfile .
docker build -t falcon-web:latest -f falcon-web/Dockerfile .
```

The `docker-compose up` target is recommended for local development and includes all services pre-configured.

## Trust Protocol & Cryptographic Verification

### Transitive Trust Model

Falcon computes trust scores through relationship graphs:

```
trust(observer → target) = Σ trust(observer → intermediate) × trust(intermediate → target)
```

This is the **Subjective Transitive Trust (STT)** model. Trust is evaluated relative to each observer, allowing for personalized trust scores without a centralized authority.

### AT Protocol Integration

Falcon natively integrates with Bluesky's AT Protocol:

* **ES256K JWT Verification**: Secp256k1 signature verification from first principles (no official Java SDK exists)
* **DID Resolution**: Cryptographic identity via Decentralized Identifiers
* **Communities & DMs**: Full support for Bluesky Communities and direct messaging
* **PDS Interoperability**: Works with any AT Protocol Personal Data Server

### Sybil Mitigation

Falcon detects coordinated inauthentic behavior via link density analysis and degrades trust scores gracefully when attacks are detected.

## Performance

Falcon is built for production use with the following characteristics:

* **JWT Verification**: ~2ms per token (Secp256k1 ES256K with caching)
* **Trust Computation**: ~50ms per query (paths up to depth 3; Dijkstra-based)
* **API Latency (P95)**: ~120ms (including database and trust lookups)
* **Throughput**: ~1000 req/s per core (scales linearly; Project Loom virtual threads)
* **CI/CD Pipeline**: 19s (Maven + Docker build + push)

## More information

* Documentation is available at [falcon.bigmoat.io](https://falcon.bigmoat.io).
* Architecture details are in [ARCHITECTURE.md](./ARCHITECTURE.md).
* See the [Community page](https://github.com/JohannaWeb/ProjectFalcon#community) for how to reach Falcon developers and users.
* Known issues are documented in the [GitHub Issues](https://github.com/JohannaWeb/ProjectFalcon/issues) tracker.

## Contributing

Refer to [CONTRIBUTING.md](./CONTRIBUTING.md)

## License

Apache License 2.0, see [LICENSE](./LICENSE).
