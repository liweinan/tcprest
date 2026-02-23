# tcprest-registry E2E Tests

## Running E2E Tests

Current E2E tests use **InMemoryRegistry** and in-process servers (SingleThreadTcpRestServer). No Docker or external services are required.

```bash
# From repo root
mvn test -pl tcprest-registry

# Or with TestNG suite (sequential)
mvn test -pl tcprest-registry -Dsurefire.suiteXmlFiles=src/test/resources/testng.xml
```

**Prerequisites:** None. Ports 26000+ are used; ensure they are free.

## Test Coverage

| Test | Description |
|------|-------------|
| DiscoveryE2ETest | Client resolves service via discovery, calls server; after deregister, NoInstanceException. |
| DiscoveryLoadBalanceE2ETest | Two instances under one service name; round-robin distributes requests. |
| RetryE2ETest | Server fails first N calls; client with RetryPolicy retries and eventually succeeds. |
| CircuitBreakerE2ETest | One instance always fails; after threshold, circuit opens and only healthy instance is used. |

## When Docker Is Required

- **Current (InMemory):** Docker is **not** required. Local and CI run the same tests without containers.
- **Future (real registry):** If E2E tests are added for Nacos, Consul, or other external registries, those tests will require:
  - **Option A:** [Testcontainers](https://testcontainers.org/) in `@BeforeClass` to start the registry image (e.g. `nacos/nacos-server`), so local and CI run the same way.
  - **Option B:** A `docker-compose.yml` that starts the registry; CI and local run `docker-compose up -d` before `mvn test`, then `docker-compose down` after.

Documentation and CI configuration will be updated when such tests are introduced. Precondition: Docker installed and available when the "real registry" test profile or class is enabled.
