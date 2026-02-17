# TcpRest Protocol Specification

This document explains the TcpRest wire protocol formats for both Protocol v1 (legacy) and Protocol v2 (current).

## Design Philosophy

TcpRest uses a simple, text-based protocol that is:
- **Human-readable**: Debugging with `tcpdump` or `wireshark` is straightforward
- **Language-agnostic**: Any language can implement a client/server
- **Backward-compatible**: New protocol versions work with old clients
- **Efficient**: Base64 encoding with optional compression

---

## Protocol v1 (Legacy)

### Request Format

```
[COMPRESSION]|ClassName/methodName(PARAMS)
```

**Components:**
- `COMPRESSION`: `0` (uncompressed) or `1` (gzip)
- `ClassName`: Fully qualified class name (e.g., `com.example.Calculator`)
- `methodName`: Method name (e.g., `add`)
- `PARAMS`: Base64-encoded parameters separated by `:::`

**Example:**
```
0|com.example.Calculator/add({{NQ==}}:::{{Mw==}})
```

Decoded: Call `Calculator.add` with params `5` and `3`

### Response Format

```
[COMPRESSION]|{{BASE64_RESULT}}
```

**Example:**
```
0|{{OA==}}
```

Decoded: Result is `8`

### Limitations

1. **No method overloading support**: Only method name is sent, not parameter types
   - If a class has `add(int, int)` and `add(double, double)`, only the first match is called
   - The extractor stops at the first name match (breaking other overloads)

2. **No exception propagation**: Exceptions are swallowed and return `NullObj`
   - Server catches `InvocationTargetException` and returns null instead of error details
   - Clients cannot distinguish between null result and server error

3. **No status codes**: Success and failure look identical
   - Empty response could mean null, error, or network issue

---

## Protocol v2 (Current)

Protocol v2 solves v1's limitations by adding **method signatures** and **status codes**.

### Request Format

```
V2|[COMPRESSION]|ClassName/methodName(TYPE_SIGNATURE)(PARAMS)
```

**Components:**
- `V2`: Version prefix for auto-detection
- `COMPRESSION`: `0` (uncompressed) or `1` (gzip)
- `ClassName`: Fully qualified class name
- `methodName`: Method name
- `TYPE_SIGNATURE`: JVM internal type signature (e.g., `(II)`, `(DD)`, `(Ljava/lang/String;)`)
- `PARAMS`: Base64-encoded parameters separated by `:::`

**Example:**
```
V2|0|com.example.Calculator/add(II)({{NQ==}}:::{{Mw==}})
```

Decoded: Call `Calculator.add(int, int)` with params `5` and `3`

### Response Format

```
V2|[COMPRESSION]|STATUS|BODY
```

**Components:**
- `STATUS`: Status code (see below)
- `BODY`: Base64-encoded result or error message

**Example (Success):**
```
V2|0|0|{{OA==}}
```

Decoded: Success, result is `8`

**Example (Business Exception):**
```
V2|0|1|{{VmFsaWRhdGlvbkV4Y2VwdGlvbjogQWdlIG11c3QgYmUgcG9zaXRpdmU=}}
```

Decoded: Business exception with message "ValidationException: Age must be positive"

### Status Codes

| Code | Name | Meaning | Client Behavior |
|------|------|---------|-----------------|
| 0 | SUCCESS | Method executed successfully | Decode and return result |
| 1 | BUSINESS_EXCEPTION | Business logic error (extends BusinessException) | Throw RemoteBusinessException |
| 2 | SERVER_ERROR | Server-side error (NullPointerException, etc.) | Throw RemoteInvocationException |
| 3 | PROTOCOL_ERROR | Malformed request or unsupported version | Throw ProtocolException |

### Type Signatures (JVM Internal Format)

Method signatures uniquely identify overloaded methods using JVM internal type descriptors.

#### Primitive Types

| Java Type | Signature | Example Method | Full Signature |
|-----------|-----------|----------------|----------------|
| int | I | add(int, int) | (II) |
| double | D | add(double, double) | (DD) |
| long | J | multiply(long, long) | (JJ) |
| float | F | calculate(float) | (F) |
| boolean | Z | isValid(boolean) | (Z) |
| byte | B | process(byte) | (B) |
| short | S | convert(short) | (S) |
| char | C | getChar(char) | (C) |

#### Object Types

| Java Type | Signature | Example Method | Full Signature |
|-----------|-----------|----------------|----------------|
| String | Ljava/lang/String; | process(String) | (Ljava/lang/String;) |
| Integer | Ljava/lang/Integer; | box(Integer) | (Ljava/lang/Integer;) |
| Custom class | Lcom/example/Foo; | handle(Foo) | (Lcom/example/Foo;) |

#### Array Types

| Java Type | Signature | Example Method | Full Signature |
|-----------|-----------|----------------|----------------|
| int[] | [I | sum(int[]) | ([I) |
| String[] | [Ljava/lang/String; | join(String[]) | ([Ljava/lang/String;) |
| int[][] | [[I | matrix(int[][]) | ([[I) |

#### Complex Examples

```java
// Method: public int add(int a, int b)
// Signature: (II)

// Method: public double add(double a, double b)
// Signature: (DD)

// Method: public String process(String s, int n, boolean flag)
// Signature: (Ljava/lang/String;IZ)

// Method: public int sum(int[] numbers)
// Signature: ([I)

// Method: public void configure(Map<String, Object> config)
// Signature: (Ljava/util/Map;)  // Generics are erased
```

### Parameter Encoding

Parameters are wrapped in `{{...}}` and separated by `:::`.

**Encoding Rules:**
- `null` → `NULL` marker (not Base64-encoded)
- Empty string `""` → Empty Base64 (distinguishable from null)
- Primitives → `toString()` then Base64
- Arrays → `Arrays.toString()` format then Base64 (e.g., `[1, 2, 3]`)
- Objects → `toString()` then Base64

**Examples:**

| Java Value | Encoded | Decoded |
|------------|---------|---------|
| `5` | `{{NQ==}}` | `"5"` |
| `null` | `{{NULL}}` | `null` |
| `""` | `{{}}` | `""` |
| `"hello"` | `{{aGVsbG8=}}` | `"hello"` |
| `new int[]{1, 2, 3}` | `{{WzEsIDIsIDNd}}` | `"[1, 2, 3]"` |

---

## Version Detection & Compatibility

### Auto-Detection

The server automatically detects protocol version by checking the request prefix:

```java
if (request.startsWith("V2|")) {
    return processV2Request(request);
} else {
    return processV1Request(request);  // Default to v1
}
```

No handshake or negotiation is needed - the protocol version is self-describing.

### Server Modes

Servers can be configured with three protocol modes:

| Mode | Accepts V1 | Accepts V2 | Use Case |
|------|------------|------------|----------|
| `ProtocolVersion.V1` | ✅ | ❌ | Legacy systems only |
| `ProtocolVersion.V2` | ❌ | ✅ | New deployments |
| `ProtocolVersion.AUTO` | ✅ | ✅ | **Default** - mixed environments |

```java
// Server configuration
TcpRestServer server = new SingleThreadTcpRestServer(8080);
server.setProtocolVersion(ProtocolVersion.AUTO);  // Default: accept both
```

### Client Configuration

Clients default to v1 for backward compatibility:

```java
// V1 client (default)
TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8080
);

// V2 client (opt-in)
TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8080
)
    .withProtocolV2();
```

### Compatibility Matrix

| Client | Server Mode | Result | Notes |
|--------|-------------|--------|-------|
| V1 | V1 only | ✅ Works | Legacy mode |
| V1 | AUTO | ✅ Works | Server accepts v1 requests |
| V1 | V2 only | ❌ Protocol error | Server rejects v1 format |
| V2 | V1 only | ❌ Protocol error | Server doesn't understand V2| prefix |
| V2 | AUTO | ✅ Works | Server accepts v2 requests |
| V2 | V2 only | ✅ Works | Recommended for new systems |

### Migration Strategy

**Phase 1: Deploy Server Updates**
- Update servers to use `ProtocolVersion.AUTO` (default)
- V1 clients continue working without changes

**Phase 2: Upgrade Clients**
- Gradually migrate clients to v2 using `.withProtocolV2()`
- Monitor for issues (old clients can still connect)

**Phase 3: (Optional) V2-Only Mode**
- Once all clients upgraded, switch servers to `ProtocolVersion.V2`
- Reject legacy v1 clients

**Rollback:** Remove `.withProtocolV2()` to revert clients to v1

---

## Exception Handling

### V1 Exception Handling

**Server Behavior:**
```java
try {
    Object result = method.invoke(instance, params);
    return encodeResult(result);
} catch (InvocationTargetException e) {
    return encodeResult(NullObj);  // Swallows exception!
}
```

**Client Receives:** Null value (cannot distinguish from legitimate null)

### V2 Exception Handling

**Server Behavior:**
```java
try {
    Object result = method.invoke(instance, params);
    return "V2|0|0|" + encodeResult(result);  // SUCCESS
} catch (BusinessException e) {
    return "V2|0|1|" + encodeException(e);    // BUSINESS_EXCEPTION
} catch (Exception e) {
    return "V2|0|2|" + encodeException(e);    // SERVER_ERROR
}
```

**Client Receives:** Proper exception with type and message

**Exception Categories:**

1. **Business Exceptions** (Status Code 1)
   - Extend `cn.huiwings.tcprest.exception.BusinessException`
   - Represent expected error conditions (validation failures, business rule violations)
   - Client receives: `RemoteBusinessException` with original message

2. **Server Errors** (Status Code 2)
   - Any other exception (NullPointerException, IllegalArgumentException, etc.)
   - Represent unexpected errors
   - Client receives: `RemoteInvocationException` with stack trace info

3. **Protocol Errors** (Status Code 3)
   - Malformed requests, unsupported versions
   - Client receives: `ProtocolException`

**Example:**

```java
// Server-side service
public class UserService {
    public void validateAge(int age) {
        if (age < 0) {
            throw new ValidationException("Age must be non-negative");
        }
    }
}

// Client-side call (V2)
try {
    userService.validateAge(-1);
} catch (RuntimeException e) {
    // Receives: RuntimeException wrapping original ValidationException
    // Message: "ValidationException: Age must be non-negative"
}
```

---

## Compression Support

Both protocols support optional gzip compression to reduce bandwidth.

### Compression Flag

| Value | Meaning | When to Use |
|-------|---------|-------------|
| 0 | Uncompressed | Small messages (<1KB) |
| 1 | Gzip compressed | Large messages (>1KB) |

### Compression Configuration

**Server:**
```java
TcpRestServer server = new SingleThreadTcpRestServer(8080);
server.enableCompression();  // Automatic compression for large responses
```

**Client:**
```java
TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8080
)
    .withCompression();  // Compress large requests
```

### Compression Threshold

Default threshold: 1024 bytes

```java
CompressionConfig config = new CompressionConfig();
config.setEnabled(true);
config.setThreshold(2048);  // Only compress if >2KB
config.setLevel(6);         // Compression level (0-9)
```

**Note:** Compression is applied AFTER Base64 encoding, so the threshold checks the encoded size.

---

## Wire Protocol Examples

### Example 1: Simple Method Call (V2)

**Java Code:**
```java
calculator.add(10, 20);
```

**Wire Request:**
```
V2|0|com.example.Calculator/add(II)({{MTA=}}:::{{MjA=}})
```

**Breakdown:**
- `V2`: Protocol version 2
- `0`: Uncompressed
- `com.example.Calculator`: Class name
- `/add`: Method name
- `(II)`: Signature for `add(int, int)`
- `{{MTA=}}`: Base64(`"10"`)
- `:::`: Parameter separator
- `{{MjA=}}`: Base64(`"20"`)

**Wire Response:**
```
V2|0|0|{{MzA=}}
```

**Breakdown:**
- `V2|0`: Version + uncompressed
- `0`: SUCCESS status
- `{{MzA=}}`: Base64(`"30"`)

### Example 2: Method Overloading (V2)

**Java Code:**
```java
calculator.add(2.5, 3.5);  // Different overload!
```

**Wire Request:**
```
V2|0|com.example.Calculator/add(DD)({{Mi41}}:::{{My41}})
```

**Key Difference:**
- Signature is `(DD)` not `(II)` - server finds correct overload

### Example 3: Exception Handling (V2)

**Java Code:**
```java
validator.validateAge(-5);
```

**Wire Request:**
```
V2|0|com.example.Validator/validateAge(I)({{LTU=}})
```

**Wire Response (Error):**
```
V2|0|1|{{VmFsaWRhdGlvbkV4Y2VwdGlvbjogQWdlIG11c3QgYmUgbm9uLW5lZ2F0aXZl}}
```

**Breakdown:**
- `1`: BUSINESS_EXCEPTION status
- Body: Base64(`"ValidationException: Age must be non-negative"`)

### Example 4: Array Parameter (V2)

**Java Code:**
```java
calculator.sum(new int[]{1, 2, 3, 4, 5});
```

**Wire Request:**
```
V2|0|com.example.Calculator/sum([I)({{WzEsIDIsIDMsIDQsIDVd}})
```

**Breakdown:**
- `([I)`: Signature for `sum(int[])`
- `{{WzEsIDIsIDMsIDQsIDVd}}`: Base64(`"[1, 2, 3, 4, 5]"`)

**Wire Response:**
```
V2|0|0|{{MTU=}}
```

Result: `15`

---

## Implementation Notes

### Type Signature Generation

The `TypeSignatureUtil` class generates JVM signatures from Java `Method` objects:

```java
Method method = Calculator.class.getMethod("add", int.class, int.class);
String signature = TypeSignatureUtil.getMethodSignature(method);
// Returns: "(II)"
```

### Method Lookup

Finding the exact overload:

```java
Class<?> clazz = Calculator.class;
String methodName = "add";
String signature = "(DD)";

Method method = TypeSignatureUtil.findMethodBySignature(clazz, methodName, signature);
// Returns: public double add(double, double)
```

### Interface-to-Implementation Mapping

The server automatically maps interface calls to registered implementations:

```java
// Client sends: "com.example.MyService/doWork(I)"
// Server looks up: registered instance implementing MyService
// Server finds: MyServiceImpl instance and invokes it
```

No explicit registration of implementation class name is needed.

---

## Performance Considerations

### Protocol Overhead

**V1 Request Overhead:** ~50 bytes + Base64 encoding (33% inflation)
**V2 Request Overhead:** ~60 bytes + Base64 encoding + signature (~10-30 bytes)

**Example Comparison:**

| Payload | V1 Size | V2 Size | Overhead Increase |
|---------|---------|---------|-------------------|
| 100 bytes | ~183 bytes | ~203 bytes | +11% |
| 1KB | ~1.4KB | ~1.42KB | +1.4% |
| 10KB | ~13.4KB | ~13.42KB | +0.15% |

**Conclusion:** V2 overhead is negligible for typical use cases.

### Signature Caching

Type signatures are computed once per method and cached:

```java
// First call: computes signature
Method method = clazz.getMethod("add", int.class, int.class);
String sig1 = TypeSignatureUtil.getMethodSignature(method);  // ~100μs

// Subsequent calls: uses cached value
String sig2 = TypeSignatureUtil.getMethodSignature(method);  // ~1μs
```

### Compression Trade-offs

| Message Size | Compress? | Reason |
|--------------|-----------|--------|
| <1KB | No | Compression overhead > savings |
| 1-10KB | Maybe | Depends on data (text vs binary) |
| >10KB | Yes | Significant bandwidth savings |

Typical compression ratios for text data: 60-80% reduction

---

## Security Considerations

### 1. Class Name Validation

**Risk:** Malicious clients could send arbitrary class names
**Mitigation:** Only registered services are accessible

```java
// Client sends: "java.lang.Runtime/exec(...)"
// Server: No such resource registered → ProtocolException
```

### 2. Method Access Control

**Risk:** Invoking private or protected methods
**Mitigation:** Only public methods of registered interfaces are callable

### 3. Exception Information Leakage

**V1 Risk:** Swallows exceptions (no info leak, but poor UX)
**V2 Risk:** Sends exception messages to client

**Mitigation:** Sanitize sensitive info in exception messages

```java
// Bad: throw new Exception("Database password: " + pwd)
// Good: throw new Exception("Database connection failed")
```

### 4. Denial of Service

**Risk:** Large compressed payloads (zip bomb)
**Mitigation:** Enforce size limits before decompression

```java
CompressionConfig config = new CompressionConfig();
config.setMaxSize(10 * 1024 * 1024);  // 10MB limit
```

---

## Future Protocol Versions

### Potential V3 Features

- **Binary protocol option**: Reduce Base64 overhead
- **Streaming support**: Large file transfers
- **Multiplexing**: Multiple requests over one connection
- **Authentication headers**: Built-in auth support
- **Schema validation**: JSON Schema or Protocol Buffers

### Versioning Strategy

- All new versions will use prefix-based detection (e.g., `V3|...`)
- `ProtocolVersion.AUTO` will support all versions
- Deprecated versions will be supported for minimum 2 major releases

---

## Appendix: Base64 Encoding Reference

TcpRest uses standard Base64 (RFC 4648) for parameter encoding.

**Encoding Examples:**

| Input | Base64 Output |
|-------|---------------|
| `5` | `NQ==` |
| `Hello` | `SGVsbG8=` |
| `[1, 2, 3]` | `WzEsIDIsIDNd` |
| `true` | `dHJ1ZQ==` |
| `3.14` | `My4xNA==` |

**Why Base64?**
- Safe for text-based protocols (no control characters)
- Language-agnostic encoding/decoding
- Simple to debug (easily decoded with command-line tools)

**Command-line Decoding:**
```bash
# Decode a parameter
echo "SGVsbG8=" | base64 -d
# Output: Hello
```

---

## References

- **JVM Type Signatures:** [Java VM Specification §4.3](https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.3)
- **Base64 Encoding:** [RFC 4648](https://datatracker.ietf.org/doc/html/rfc4648)
- **Gzip Compression:** [RFC 1952](https://datatracker.ietf.org/doc/html/rfc1952)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-18
**TcpRest Version:** 1.1.0+
