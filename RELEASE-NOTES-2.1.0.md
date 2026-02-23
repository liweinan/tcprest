# TcpRest 2.1.0.Final Release Notes

## Overview

2.1.0.Final adds **service discovery and governance** on top of 2.0.x, plus optional adapter modules and a comprehensive E2E test suite.

## New Features

### Service Discovery and Governance (tcprest-commons + tcprest-registry)

- **Interfaces** (commons, zero deps): `ServiceRegistry`, `ServiceDiscovery`, `LoadBalancer`, `HostPort`, `RetryPolicy`, `CircuitBreaker`, `CircuitBreakerProvider`; default `RoundRobinLoadBalancer`.
- **Server**: `setServiceRegistry(registry, serviceName, advertisedHost)`; automatic register/deregister on `up()`/`down()`.
- **Client**: `TcpRestClientFactory(discovery, serviceName, loadBalancer, ...)` for discovery, load balancing, and optional retry/circuit breaker.
- **tcprest-registry**: `InMemoryRegistry`, `SimpleRetryPolicy`, `CircuitBreakerImpl`, `PerInstanceCircuitBreakerProvider`; E2E tests (discovery, load balance, retry, circuit breaker) require no Docker.

### Registry Adapters

- **tcprest-nacos**: `NacosRegistry` implements `ServiceRegistry`/`ServiceDiscovery` via Nacos NamingService; Testcontainers E2E (`NacosRegistryE2ETest`).
- **tcprest-consul**: `ConsulRegistry` implements the same interfaces via Consul Agent + Health API; Testcontainers E2E (`ConsulRegistryE2ETest`).

### Governance Adapter (Resilience4j)

- **tcprest-resilience4j**: `Resilience4jRetryPolicy(RetryConfig)`, `Resilience4jCircuitBreakerAdapter`, `Resilience4jCircuitBreakerProvider` implement Commons’ `RetryPolicy`/`CircuitBreaker`/`CircuitBreakerProvider` with advanced policies (e.g. exponential backoff).

### Comprehensive E2E and CI

- **tcprest-e2e**: E2E module covering discovery (InMemoryRegistry) + Resilience4j retry/circuit breaker + Netty server + SSL mutual auth + compression; `ComprehensiveE2ETest`, no Docker required.
- **CI**: Full test run in CI; Nacos/Consul E2E via Testcontainers; optional `docker-compose.test.yml` for local runs.
- **Port conflict fix**: `CompressionIntegrationTest` uses `PortGenerator.from(35000)` instead of Random to avoid port clashes with other tests or processes.

## Dependencies and Modules

- **tcprest-commons**: remains **zero runtime dependencies**.
- New optional modules: tcprest-registry, tcprest-nacos, tcprest-consul, tcprest-resilience4j, tcprest-e2e; add as needed.

## Upgrade Notes

- **Backward compatible** with 2.0.x: direct connect and protocol unchanged; new features are optional (discovery, retry, circuit breaker, new modules).
- To use discovery: call `setServiceRegistry` on the server before `up()`; use the factory constructor with `discovery`/`serviceName` on the client.

## Full Changelog (since 2.0.1.Final)

- feat: registry, discovery, and governance (retry, circuit breaker)
- Add tcprest-nacos and tcprest-consul with Testcontainers E2E and CI/docs
- Add tcprest-resilience4j: Resilience4j adapters for RetryPolicy and CircuitBreaker
- Add tcprest-e2e module and fix CompressionIntegrationTest port conflict
