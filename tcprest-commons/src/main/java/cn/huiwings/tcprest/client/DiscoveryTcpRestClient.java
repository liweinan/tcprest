package cn.huiwings.tcprest.client;

import cn.huiwings.tcprest.discovery.HostPort;
import cn.huiwings.tcprest.ssl.SSLParams;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * TcpRestClient that resolves the target host:port per request via a {@link Supplier}&lt;{@link HostPort}&gt;.
 * Used when the client is created with {@link cn.huiwings.tcprest.discovery.ServiceDiscovery} and
 * {@link cn.huiwings.tcprest.discovery.LoadBalancer}; each {@link #sendRequest(String, int)} call
 * obtains the current address from the supplier and delegates to a fresh {@link DefaultTcpRestClient}.
 * Optional {@code afterRequest} callback is invoked with (HostPort, success) for per-instance tracking (e.g. circuit breaker).
 *
 * @since 2.0.0
 */
public class DiscoveryTcpRestClient implements TcpRestClient {

    private final String delegatedClassName;
    private final SSLParams sslParams;
    private final Supplier<HostPort> addressSupplier;
    private final BiConsumer<HostPort, Boolean> afterRequest;

    public DiscoveryTcpRestClient(String delegatedClassName, SSLParams sslParams, Supplier<HostPort> addressSupplier) {
        this(delegatedClassName, sslParams, addressSupplier, null);
    }

    public DiscoveryTcpRestClient(String delegatedClassName, SSLParams sslParams, Supplier<HostPort> addressSupplier,
                                  BiConsumer<HostPort, Boolean> afterRequest) {
        this.delegatedClassName = delegatedClassName;
        this.sslParams = sslParams;
        this.addressSupplier = addressSupplier;
        this.afterRequest = afterRequest;
    }

    @Override
    public String sendRequest(String request, int timeout) throws Exception {
        HostPort addr = addressSupplier.get();
        try {
            TcpRestClient client = new DefaultTcpRestClient(sslParams, delegatedClassName, addr.getHost(), addr.getPort());
            String result = client.sendRequest(request, timeout);
            if (afterRequest != null) {
                afterRequest.accept(addr, true);
            }
            return result;
        } catch (Exception e) {
            if (afterRequest != null) {
                afterRequest.accept(addr, false);
            }
            throw e;
        }
    }

    @Override
    public String getDeletgatedClassName() {
        return delegatedClassName;
    }
}
