package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.mapper.Mapper;

import java.util.Map;

/**
 * Interface for mapper (de)serialization registration.
 *
 * <p>Defines the contract for registering type-to-string mappers used when
 * serializing/deserializing method parameters and return values. {@link TcpRestServer}
 * extends this interface along with {@link ResourceRegister} and adds lifecycle
 * and server configuration (compression, security).</p>
 *
 * @since 1.1.0
 */
public interface MapperRegister {

    /**
     * Get a cloned copy of mappers. The implementation should ensure thread safety.
     *
     * @return map of canonical class name to mapper
     */
    Map<String, Mapper> getMappers();

    /**
     * Register a mapper for the given type (canonical class name).
     *
     * @param canonicalName fully qualified class name
     * @param mapper        mapper instance
     */
    void addMapper(String canonicalName, Mapper mapper);
}
