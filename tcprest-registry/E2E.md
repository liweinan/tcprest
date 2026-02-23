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

- **tcprest-registry (InMemory):** Docker is **not** required. Local and CI run the same tests without containers.
- **tcprest-nacos / tcprest-consul:** E2E tests use [Testcontainers](https://testcontainers.org/) to start Nacos or Consul in Docker. **Docker must be installed and available** when running:
  - `mvn test -pl tcprest-nacos` (NacosRegistryE2ETest)
  - `mvn test -pl tcprest-consul` (ConsulRegistryE2ETest)
  If Docker is not available, these tests are **skipped** (TestNG SkipException).

**CI:** GitHub Actions `ubuntu-latest` provides Docker. The main CI job runs `mvn test`, which includes all E2E tests; Nacos E2E uses fixed host ports 8848/9848 (one Nacos per runner). No extra CI job is required.

**Optional local stack:** `docker-compose -f docker-compose.test.yml up -d` starts Nacos (8858/9858) and Consul (8600→8500) for ad-hoc use; the standard E2E path is Testcontainers.
