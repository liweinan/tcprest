package cn.huiwings.tcprest.protocol.v2;

import java.lang.reflect.Method;

/**
 * Utility class for generating and parsing JVM type signatures.
 *
 * <p>This utility solves the method overloading problem by generating unique
 * signatures for methods based on their parameter types. It uses JVM internal
 * type descriptors as defined in the JVM specification.</p>
 *
 * <p><b>Type Signature Format (JVM Internal):</b></p>
 * <pre>
 * Primitive types:
 *   int     → I
 *   double  → D
 *   boolean → Z
 *   long    → J
 *   float   → F
 *   byte    → B
 *   char    → C
 *   short   → S
 *   void    → V
 *
 * Object types:
 *   String  → Ljava/lang/String;
 *   Object  → Ljava/lang/Object;
 *
 * Array types:
 *   int[]     → [I
 *   String[]  → [Ljava/lang/String;
 *   int[][]   → [[I
 *
 * Method signatures:
 *   add(int, int)           → (II)
 *   process(String, boolean)→ (Ljava/lang/String;Z)
 *   sort(int[])             → ([I)
 * </pre>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>
 * // Get method signature
 * Method method = Calculator.class.getMethod("add", int.class, int.class);
 * String sig = TypeSignatureUtil.getMethodSignature(method);
 * // Result: "(II)"
 *
 * // Find method by signature
 * Method found = TypeSignatureUtil.findMethodBySignature(
 *     Calculator.class, "add", "(DD)"
 * );
 * // Finds: add(double, double)
 * </pre>
 *
 * @since 1.1.0
 */
public final class TypeSignatureUtil {

    private TypeSignatureUtil() {
        // Utility class, prevent instantiation
    }

    /**
     * Generate method signature from Method object.
     *
     * <p>The signature includes only parameter types, not the return type.
     * Format: (Type1Type2...TypeN)</p>
     *
     * @param method the method to generate signature for
     * @return type signature string (e.g., "(II)" for add(int, int))
     * @throws NullPointerException if method is null
     */
    public static String getMethodSignature(Method method) {
        if (method == null) {
            throw new NullPointerException("Method cannot be null");
        }

        StringBuilder sig = new StringBuilder("(");
        for (Class<?> paramType : method.getParameterTypes()) {
            sig.append(getTypeDescriptor(paramType));
        }
        sig.append(")");
        return sig.toString();
    }

    /**
     * Find method by name and signature in a class.
     *
     * <p>This method searches for an exact match based on method name and
     * parameter type signature. It enables precise method selection even
     * when multiple overloaded methods exist.</p>
     *
     * @param clazz the class to search in
     * @param methodName the method name
     * @param signature the type signature (e.g., "(II)")
     * @return the matching Method object
     * @throws NoSuchMethodException if no matching method is found
     * @throws NullPointerException if clazz, methodName, or signature is null
     */
    public static Method findMethodBySignature(Class<?> clazz, String methodName, String signature)
            throws NoSuchMethodException {
        if (clazz == null) {
            throw new NullPointerException("Class cannot be null");
        }
        if (methodName == null) {
            throw new NullPointerException("Method name cannot be null");
        }
        if (signature == null) {
            throw new NullPointerException("Signature cannot be null");
        }

        // Search in declared methods (includes private, protected, public)
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) &&
                getMethodSignature(method).equals(signature)) {
                return method;
            }
        }

        // Search in public methods from superclasses and interfaces
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) &&
                getMethodSignature(method).equals(signature)) {
                return method;
            }
        }

        throw new NoSuchMethodException(
            "No method found: " + clazz.getName() + "." + methodName + signature
        );
    }

    /**
     * Get JVM type descriptor for a class.
     *
     * <p>Converts Java Class objects to JVM internal type descriptors.
     * Handles primitives, objects, and arrays.</p>
     *
     * @param clazz the class to get descriptor for
     * @return JVM type descriptor (e.g., "I" for int, "Ljava/lang/String;" for String)
     * @throws NullPointerException if clazz is null
     */
    public static String getTypeDescriptor(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("Class cannot be null");
        }

        // Handle arrays
        if (clazz.isArray()) {
            return "[" + getTypeDescriptor(clazz.getComponentType());
        }

        // Handle primitives
        if (clazz.isPrimitive()) {
            return getPrimitiveDescriptor(clazz);
        }

        // Handle objects
        return "L" + clazz.getName().replace('.', '/') + ";";
    }

    /**
     * Get primitive type descriptor.
     *
     * @param clazz primitive class
     * @return single-character descriptor
     * @throws IllegalArgumentException if clazz is not a primitive type
     */
    private static String getPrimitiveDescriptor(Class<?> clazz) {
        if (clazz == int.class) {
            return "I";
        } else if (clazz == double.class) {
            return "D";
        } else if (clazz == boolean.class) {
            return "Z";
        } else if (clazz == long.class) {
            return "J";
        } else if (clazz == float.class) {
            return "F";
        } else if (clazz == byte.class) {
            return "B";
        } else if (clazz == char.class) {
            return "C";
        } else if (clazz == short.class) {
            return "S";
        } else if (clazz == void.class) {
            return "V";
        } else {
            throw new IllegalArgumentException("Not a primitive type: " + clazz.getName());
        }
    }

    /**
     * Parse parameter types from a type signature.
     *
     * <p>Converts a type signature string back into an array of Class objects.
     * This is useful for reconstructing method parameter types from signatures.</p>
     *
     * @param signature the type signature (e.g., "(ILjava/lang/String;)")
     * @return array of Class objects representing parameter types
     * @throws ClassNotFoundException if a type in the signature cannot be found
     * @throws IllegalArgumentException if signature format is invalid
     */
    public static Class<?>[] parseParameterTypes(String signature) throws ClassNotFoundException {
        if (signature == null || signature.isEmpty()) {
            throw new IllegalArgumentException("Signature cannot be null or empty");
        }

        if (!signature.startsWith("(") || !signature.contains(")")) {
            throw new IllegalArgumentException("Invalid signature format: " + signature);
        }

        // Extract content between parentheses
        int endIndex = signature.indexOf(')');
        String params = signature.substring(1, endIndex);

        if (params.isEmpty()) {
            return new Class<?>[0]; // No parameters
        }

        // Parse individual type descriptors
        java.util.List<Class<?>> types = new java.util.ArrayList<>();
        int i = 0;
        while (i < params.length()) {
            char c = params.charAt(i);

            if (c == 'L') {
                // Object type: Ljava/lang/String;
                int semicolon = params.indexOf(';', i);
                if (semicolon == -1) {
                    throw new IllegalArgumentException("Malformed object type in signature: " + params);
                }
                String className = params.substring(i + 1, semicolon).replace('/', '.');
                types.add(Class.forName(className));
                i = semicolon + 1;
            } else if (c == '[') {
                // Array type: [I or [Ljava/lang/String;
                int arrayStart = i;
                i++; // Move past '['

                // Count array dimensions
                while (i < params.length() && params.charAt(i) == '[') {
                    i++;
                }

                // Get component type
                String componentDescriptor;
                if (params.charAt(i) == 'L') {
                    int semicolon = params.indexOf(';', i);
                    componentDescriptor = params.substring(arrayStart, semicolon + 1);
                    i = semicolon + 1;
                } else {
                    // Primitive array
                    componentDescriptor = params.substring(arrayStart, i + 1);
                    i++;
                }

                types.add(descriptorToClass(componentDescriptor));
            } else {
                // Primitive type: I, D, Z, etc.
                types.add(primitiveDescriptorToClass(c));
                i++;
            }
        }

        return types.toArray(new Class<?>[0]);
    }

    /**
     * Convert type descriptor to Class object.
     *
     * @param descriptor JVM type descriptor
     * @return corresponding Class object
     * @throws ClassNotFoundException if type cannot be found
     */
    private static Class<?> descriptorToClass(String descriptor) throws ClassNotFoundException {
        if (descriptor.length() == 1) {
            return primitiveDescriptorToClass(descriptor.charAt(0));
        }

        if (descriptor.startsWith("[")) {
            // Array type - use Class.forName with descriptor directly
            return Class.forName(descriptor.replace('/', '.'));
        }

        if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
            String className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
            return Class.forName(className);
        }

        throw new IllegalArgumentException("Invalid type descriptor: " + descriptor);
    }

    /**
     * Convert primitive descriptor character to Class object.
     *
     * @param descriptor single character primitive descriptor
     * @return primitive Class object
     * @throws IllegalArgumentException if descriptor is invalid
     */
    private static Class<?> primitiveDescriptorToClass(char descriptor) {
        switch (descriptor) {
            case 'I': return int.class;
            case 'D': return double.class;
            case 'Z': return boolean.class;
            case 'J': return long.class;
            case 'F': return float.class;
            case 'B': return byte.class;
            case 'C': return char.class;
            case 'S': return short.class;
            case 'V': return void.class;
            default:
                throw new IllegalArgumentException("Invalid primitive descriptor: " + descriptor);
        }
    }
}
