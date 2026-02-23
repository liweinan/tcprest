# tcprest-e2e

Comprehensive end-to-end tests combining:

- **Service discovery**: InMemoryRegistry, RoundRobinLoadBalancer
- **Resilience4j**: RetryPolicy (RetryConfig), CircuitBreakerProvider (CircuitBreakerConfig)
- **Netty server**: NettyTcpRestServer
- **SSL/TLS**: Mutual authentication (server + client certs)
- **Compression**: GZIP enabled on server and client

## Running

From repo root:

```bash
mvn test -pl tcprest-e2e
```

Requires no Docker. SSL keystores (`server_ks`, `client_ks`) are taken from `tcprest-netty/src/test/resources` via Maven testResources.

## Test

- **ComprehensiveE2ETest**: One Netty server with SSL + compression, registered to InMemoryRegistry; client uses discovery + Resilience4j retry/circuit breaker + SSL + compression. Asserts echo, add, and large payload over the full stack.
