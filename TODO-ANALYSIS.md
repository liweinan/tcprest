# TODO Analysis Report

**Date**: 2026-02-17
**Project**: tcprest
**Analysis Scope**: All TODO comments in codebase

## Executive Summary

The codebase contains 6 TODO items. After detailed analysis and testing:
- **2 items** require fixing (deferred to future releases)
- **2 items** should be removed (current implementation sufficient)
- **1 item** cleaned up (empty comment)
- **1 item** documented as design limitation

---

## 1. NettyTcpRestServer Large Data Handling
**File**: `tcprest-netty/src/test/java/cn/huiwings/tcprest/test/smoke/TcpClientFactorySmokeTest.java:40`

### Status: ✅ FIXED (Completed: 2026-02-18)

### Problem (Historical)
NettyTcpRestServer failed when handling large payloads (>10KB) due to request fragmentation in Netty 3.10.6.

### Solution Implemented
**Netty upgraded from 3.10.6.Final → 4.1.131.Final** with proper frame handling:

```java
// Fixed with LineBasedFrameDecoder
pipeline.addLast("lineFramer", new LineBasedFrameDecoder(1024 * 1024)); // 1MB max
pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
pipeline.addLast("tcpRestProtocolHandler", new NettyTcpRestProtocolHandler(this));
```

### Changes Made
1. **Complete Netty 4.x migration**:
   - ServerBootstrap configuration updated
   - ChannelFactory → NioEventLoopGroup (boss/worker pattern)
   - ChannelPipelineFactory → ChannelInitializer
   - SimpleChannelHandler → SimpleChannelInboundHandler<String>
   - Each channel gets its own handler instance (fixes @Sharable requirement)

2. **Frame handling**: LineBasedFrameDecoder aggregates fragmented messages before decoding

3. **SSL support added**: NettyTcpRestServer now supports SSL/TLS via Netty's SslContext

### Test Results
```bash
✅ All servers now handle large payloads (>10KB):
   - SingleThreadTcpRestServer: PASS
   - NioTcpRestServer: PASS
   - NettyTcpRestServer: PASS (fixed!)

✅ All 70 tests passing (tcprest-core: 40/40, tcprest-netty: 30/30)
```

### Impact
- NettyTcpRestServer now production-ready for large payloads
- Netty 4.x provides better performance and modern API
- SSL support complete across all three server implementations

---

## 2. Protocol v2 Design (Method Overloading + Exception Handling)
**Files**:
- `tcprest-core/src/main/java/cn/huiwings/tcprest/extractor/DefaultExtractor.java:129`
- `tcprest-core/src/main/java/cn/huiwings/tcprest/protocol/TcpRestProtocol.java`

### Status: TO BE DESIGNED (Priority: Medium - unified solution needed)

### Problems Requiring Protocol Changes

**Problem A: Method Overloading Not Supported**
```java
public interface Calculator {
    int add(int a, int b);
    double add(double a, double b);  // ❌ Cannot be invoked via RPC
}
```
Current protocol only includes method names, not parameter type signatures.

**Problem B: No Server-Side Exception Handling**
```java
// Server-side exception occurs
try {
    response = processRequest(request);
} catch (Exception e) {
    e.printStackTrace();  // Client receives nothing or timeout
}
```
Current protocol has no status code mechanism to indicate success/failure.

### Current Protocol v1 Limitations

**Request format**:
```
ClassName/methodName({{base64_param1}}:::{{base64_param2}})
```

**Response format**:
```
{{base64_encoded_result}}
```

**Limitations**:
1. ❌ No method signature (cannot distinguish overloaded methods)
2. ❌ No status code (cannot indicate error vs success)
3. ❌ No version negotiation (cannot evolve protocol)
4. ❌ No metadata support (cannot add headers/compression flags)

### Proposed Protocol v2 Design

**Request format**:
```
VERSION|COMPRESSION|ClassName/methodName(TypeSignature)(params)

Example:
2|0|Calculator/add(II)({{MQ==}}:::{{Mg==}})          # add(int,int)
2|0|Calculator/add(DD)({{MS4w}}:::{{Mi4w}})          # add(double,double)
2|1|DataService/process(Ljava/lang/String;)([gzip]) # compressed
```

**Response format**:
```
VERSION|COMPRESSION|STATUS|BODY

Examples:
2|0|0|{{result}}           # Success, uncompressed
2|0|1|{{error_message}}    # Error (business logic exception)
2|0|2|{{error_message}}    # Error (server internal exception)
2|1|0|[gzip_data]          # Success, compressed
```

**Status codes**:
- `0`: Success
- `1`: Business logic exception (e.g., validation error, domain exception)
- `2`: Server internal exception (e.g., NullPointerException, database error)
- `3`: Protocol error (e.g., malformed request, unsupported version)

**Type signature format** (JVM internal representation):
- `I` = int
- `J` = long
- `D` = double
- `Z` = boolean
- `Ljava/lang/String;` = String
- `[I` = int[]
- Example: `add(Ljava/lang/String;IZ)` = `add(String s, int i, boolean b)`

### Benefits of Protocol v2

1. ✅ **Method overloading support**: Type signatures uniquely identify methods
2. ✅ **Exception propagation**: Status codes allow server to report errors gracefully
3. ✅ **Version negotiation**: Client and server can negotiate protocol version
4. ✅ **Future extensibility**: Can add metadata fields without breaking compatibility
5. ✅ **Better debugging**: Clear error codes vs silent failures

### Implementation Plan

**Phase 1: Design & Specification** (2-3 days)
1. Finalize protocol v2 specification document
2. Define version negotiation handshake
3. Design backward compatibility layer (v1 fallback)

**Phase 2: Core Implementation** (5-7 days)
1. Create `ProtocolV2Converter` and `ProtocolV2Extractor`
2. Implement status code handling in `TcpRestServer`
3. Add exception mappers for common exception types
4. Update `TcpRestClientProxy` to handle status codes

**Phase 3: Version Negotiation** (2-3 days)
1. Add version negotiation handshake on connection
2. Support both v1 and v2 protocols simultaneously
3. Add server config flag: `setProtocolVersion(1)` or `setProtocolVersion(2)`

**Phase 4: Testing & Migration** (3-4 days)
1. Add comprehensive protocol v2 tests
2. Create migration guide for v1 users
3. Update all documentation

**Total estimated effort**: 12-17 days

### Backward Compatibility Strategy

**Option A: Auto-detection** (Recommended)
```java
// Server detects protocol version from first character
if (request.startsWith("2|")) {
    return protocolV2Handler.process(request);
} else {
    return protocolV1Handler.process(request);  // Legacy support
}
```

**Option B: Negotiation handshake**
```java
// Client initiates connection
Client → Server: "TCPREST_HELLO|2"
Server → Client: "TCPREST_OK|2" or "TCPREST_OK|1" (fallback)
// Then use agreed version for all subsequent requests
```

### Workarounds Until v2 Available

**For method overloading**:
```java
// ❌ Don't use overloading
int add(int a, int b);
double add(double a, double b);

// ✅ Use distinct names
int addIntegers(int a, int b);
double addDecimals(double a, double b);
```

**For exception handling**:
```java
// ✅ Return error objects instead of throwing
public Result<Data> processOrder(Order order) {
    try {
        Data data = process(order);
        return Result.success(data);
    } catch (ValidationException e) {
        return Result.error(e.getMessage());
    }
}
```

---

## 3. Exception Transfer Enhancement
**File**: `tcprest-core/src/main/java/cn/huiwings/tcprest/mapper/MapperHelper.java:43`

### Status: NO ACTION NEEDED (TODO Removed)

### Current Implementation
```java
public class ExceptionMapper implements Mapper {
    public Object stringToObject(String param) {
        return new Exception(param);
    }
    public String objectToString(Object object) {
        return ((Exception) object).getMessage();
    }
}
```

### Analysis
- Current implementation transfers exception **messages** (sufficient for most use cases)
- TODO comment suggested transferring full stack traces
- **Decision**: Transferring stack traces over network has security implications:
  - Exposes internal implementation details
  - May leak file paths, class names, sensitive data
  - Large payload overhead

### Conclusion
Current implementation is adequate. Exception messages provide sufficient error context for RPC clients.

---

## 4. Resource Validation on Registration
**File**: `tcprest-core/src/main/java/cn/huiwings/tcprest/server/SingleThreadTcpRestServer.java:35`

### Status: IMPLEMENTED ✅

### Problem
Previously, no validation when registering resources:
```java
server.addResource(MyResource.class);
// No check if methods are serializable
```

Runtime errors occurred when unmappable types were encountered.

### Solution Implemented
Added validation in `AbstractTcpRestServer.addResource()`:
- Validates all public methods
- Checks parameter types have mappers
- Checks return type has mapper
- Logs warnings for unsupported types

```java
public void addResource(Class clazz) {
    validateResourceClass(clazz);  // ✅ Validation added
    resourceClasses.put(clazz.getCanonicalName(), clazz);
}
```

### Benefits
- **Fail-fast**: Errors caught at startup, not runtime
- **Clear diagnostics**: Detailed warning messages
- **Better DX**: Developers get immediate feedback

---

## 5. Singleton Annotation Documentation
**File**: `tcprest-core/src/main/java/cn/huiwings/tcprest/annotations/Singleton.java:4`

### Status: DOCUMENTED ✅

### Issue
Empty TODO comment with no context.

### Solution
Added comprehensive JavaDoc explaining annotation purpose and usage.

```java
/**
 * Marks a resource class to be managed as a singleton by the server.
 * ...
 */
@Server
public @interface Singleton {
}
```

---

## Summary Table

| # | Location | Issue | Action | Status |
|---|----------|-------|--------|--------|
| 1 | TcpClientFactorySmokeTest.java:40 | Netty large data | **Fixed** | ✅ Completed (2026-02-18) |
| 2 | DefaultExtractor.java:129 + protocol | Protocol v2 (overloading + exceptions) | **Design needed** | Medium priority (unified solution) |
| 3 | MapperHelper.java:43 | Exception transfer | **Remove TODO** | N/A |
| 4 | AbstractTcpRestServer validation | Resource validation | **Implemented** | ✅ Completed |
| 5 | Singleton.java:4 | Documentation | **Documented** | ✅ Completed |

**Note**: Original TODO #2 (method overloading) and #4 (server-side exception handling) have been merged into a unified Protocol v2 design task.

---

## Recommendations

### For Next Release
1. Create GitHub Issue for Protocol v2 design (TODO #2)
2. ✅ ~~Add "Known Limitations" section to README.md~~ (Completed)
3. Add coding guidelines discouraging method overloading
4. Document current workarounds for exception handling

### For Version 2.0 (Breaking Changes Allowed)
1. **Protocol v2 design and implementation** (TODO #2):
   - Method overloading support via type signatures
   - Exception/error handling with status codes
   - Version negotiation mechanism
   - Backward compatibility with v1
2. ✅ ~~Netty 4.x upgrade (fixes TODO #1)~~ (Completed: 4.1.131.Final)

### Documentation Updates Needed
- README.md: Add "Known Limitations" section
- JavaDoc: Document method overloading restriction
- ARCHITECTURE.md: Explain protocol design decisions

---

## Testing Notes

All changes validated:
```bash
✅ tcprest-core: 40/40 tests passing
✅ tcprest-netty: 30/30 tests passing (includes Netty 4.x tests)
✅ BUILD SUCCESS
✅ Total: 70/70 tests passing
```

---

**Report Generated**: 2026-02-17
**Last Updated**: 2026-02-18 (Netty 4.x upgrade completed, Protocol v2 design consolidated)
**Reviewed By**: Claude Sonnet 4.5
**Status**: 3/5 items completed, 1 pending (Protocol v2 - medium priority unified solution)
