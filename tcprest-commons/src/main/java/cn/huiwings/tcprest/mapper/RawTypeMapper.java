package cn.huiwings.tcprest.mapper;

import java.io.*;
import java.util.Base64;

/**
 * Auto-serialization mapper for {@link java.io.Serializable} objects using Java serialization.
 *
 * <p>RawTypeMapper provides automatic serialization/deserialization for any Java object
 * that implements the {@link java.io.Serializable} interface. This enables zero-configuration
 * support for complex objects without requiring custom mapper implementation.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Zero configuration</b> - Works automatically for Serializable objects</li>
 *   <li><b>Handles complex graphs</b> - Nested objects, collections, inheritance</li>
 *   <li><b>Type preservation</b> - Exact class type is preserved (Car → Car, not Vehicle)</li>
 *   <li><b>Transient support</b> - Fields marked transient are excluded</li>
 * </ul>
 *
 * <p><b>Usage in V2 Protocol:</b></p>
 * <p>In Protocol V2, RawTypeMapper is used automatically at Priority 2 for:</p>
 * <ol>
 *   <li>Collection interfaces (List, Map, Set) - deserializes to actual implementations</li>
 *   <li>Serializable classes - any custom class implementing Serializable</li>
 * </ol>
 *
 * <p><b>Example - Automatic Usage:</b></p>
 * <pre>
 * // DTO class - just implement Serializable
 * public class User implements Serializable {
 *     private static final long serialVersionUID = 1L;
 *     private String name;
 *     private int age;
 *     private transient String password;  // Excluded from serialization
 * }
 *
 * // Service interface
 * public interface UserService {
 *     User getUser(int id);  // Works automatically!
 * }
 *
 * // No mapper registration needed - RawTypeMapper is used automatically
 * </pre>
 *
 * <p><b>Example - Manual Usage:</b></p>
 * <pre>
 * RawTypeMapper mapper = new RawTypeMapper();
 *
 * // Serialize object to Base64 string
 * User user = new User("Alice", 30);
 * String base64 = mapper.objectToString(user);
 * // → "rO0ABXNyABFjb20uZXhhbXBsZS5Vc2VyAAAAAAAAAAECAAJJAANhZ2VMAANuYW1l..."
 *
 * // Deserialize Base64 string back to object
 * User decoded = (User) mapper.stringToObject(base64);
 * // → User(name="Alice", age=30)
 * </pre>
 *
 * <p><b>Supported Scenarios:</b></p>
 * <ul>
 *   <li><b>Nested objects</b>: Person → Address → City (all must be Serializable)</li>
 *   <li><b>Inheritance</b>: Car extends Vehicle (exact type preserved)</li>
 *   <li><b>Collections</b>: ArrayList, HashMap, TreeSet (via Serializable implementations)</li>
 *   <li><b>Arrays</b>: Object[], User[], etc.</li>
 * </ul>
 *
 * <p><b>Format:</b></p>
 * <p>Output is standard Java serialization Base64-encoded:</p>
 * <pre>
 * Binary serialization → Base64 encoding → String
 * rO0ABXNyABFjb20uZXhhbXBsZS5Vc2VyAAAAAAAAAAECAAJJAANhZ2VMAANuYW1l...
 * </pre>
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li><b>Java-only</b> - Not compatible with non-Java clients</li>
 *   <li><b>Binary format</b> - Not human-readable (use JSON mapper for debugging)</li>
 *   <li><b>Size overhead</b> - Includes class metadata (larger than JSON for simple data)</li>
 *   <li><b>Version sensitivity</b> - Requires serialVersionUID for class evolution</li>
 * </ul>
 *
 * <p><b>When to Use:</b></p>
 * <ul>
 *   <li>✅ Internal Java microservices</li>
 *   <li>✅ Rapid prototyping</li>
 *   <li>✅ Complex object graphs</li>
 *   <li>❌ Public APIs (use JSON mapper instead)</li>
 *   <li>❌ Cross-language services</li>
 * </ul>
 *
 * <p><b>Best Practices:</b></p>
 * <ul>
 *   <li>Always define serialVersionUID for version control</li>
 *   <li>Mark sensitive fields as transient</li>
 *   <li>Ensure all nested objects are Serializable</li>
 *   <li>For public APIs, prefer custom JSON mappers (Gson/Jackson)</li>
 * </ul>
 *
 * @author Weinan Li
 * @since 1.0.0
 * @see Mapper
 * @see java.io.Serializable
 * @see cn.huiwings.tcprest.parser.v2.ProtocolV2Parser
 */
public class RawTypeMapper implements Mapper {
    /**
     * Deserialize Base64-encoded Java serialization data back to object.
     *
     * <p>Converts Base64 string (containing Java serialization bytes) back to
     * the original Java object using {@link java.io.ObjectInputStream}.</p>
     *
     * <p><b>Process:</b></p>
     * <pre>
     * Base64 string → decode → binary bytes → deserialize → Java object
     * </pre>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * String base64 = "rO0ABXNyABFjb20uZXhhbXBsZS5Vc2VyAAAAAAAAAAECAAJJAANhZ2U...";
     * User user = (User) mapper.stringToObject(base64);
     * // → User object with all fields restored
     * </pre>
     *
     * @param param Base64-encoded serialization data (must not be null)
     * @return deserialized Java object (preserves exact type), or null if deserialization fails
     */
    /** Reject known dangerous classes during deserialization (mitigates deserialization of user-controlled data). */
    private static final ObjectInputFilter DESERIALIZATION_FILTER = info -> {
        Class<?> serialClass = info.serialClass();
        if (serialClass == null) return ObjectInputFilter.Status.ALLOWED;
        String name = serialClass.getName();
        boolean reject = name.equals("java.lang.ProcessBuilder")
                || name.equals("java.lang.Runtime")
                || name.startsWith("javax.management.")
                || name.startsWith("java.util.prefs.")
                || name.startsWith("java.awt.")
                || name.startsWith("javax.swing.")
                || name.startsWith("com.sun.")
                || name.startsWith("sun.");
        return reject ? ObjectInputFilter.Status.REJECTED : ObjectInputFilter.Status.UNDECIDED;
    };

    @Override
    public Object stringToObject(String param) {
        try {
            ByteArrayInputStream source = new ByteArrayInputStream(Base64.getDecoder().decode(param));
            ObjectInputStream is = new ObjectInputStream(source);
            is.setObjectInputFilter(DESERIALIZATION_FILTER);
            return is.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Serialize Java object to Base64-encoded string using Java serialization.
     *
     * <p>Converts any {@link java.io.Serializable} object to a Base64 string
     * containing the binary Java serialization data using {@link java.io.ObjectOutputStream}.</p>
     *
     * <p><b>Process:</b></p>
     * <pre>
     * Java object → serialize → binary bytes → Base64 encode → String
     * </pre>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * User user = new User("Alice", 30);
     * String base64 = mapper.objectToString(user);
     * // → "rO0ABXNyABFjb20uZXhhbXBsZS5Vc2VyAAAAAAAAAAECAAJJAANhZ2U..."
     * </pre>
     *
     * <p><b>Requirements:</b></p>
     * <ul>
     *   <li>Object must implement {@link java.io.Serializable}</li>
     *   <li>All nested objects must also be Serializable</li>
     *   <li>Fields marked transient are excluded</li>
     * </ul>
     *
     * @param object the object to serialize (must implement Serializable)
     * @return Base64-encoded serialization string, or null if serialization fails
     */
    @Override
    public String objectToString(Object object) {
        try {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(target);
            os.writeObject(object);
            os.flush();
            os.close();
            return Base64.getEncoder().encodeToString(target.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
