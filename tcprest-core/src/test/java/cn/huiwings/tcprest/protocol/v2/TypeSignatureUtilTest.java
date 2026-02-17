package cn.huiwings.tcprest.protocol.v2;

import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.testng.Assert.*;

/**
 * Test class for TypeSignatureUtil.
 *
 * Tests cover:
 * - Signature generation for primitives
 * - Signature generation for objects and arrays
 * - Finding overloaded methods by signature
 * - Edge cases (nested arrays, varargs, no parameters)
 */
public class TypeSignatureUtilTest {

    // ========== Test Signature Generation for Primitives ==========

    @Test
    public void testGetMethodSignature_primitiveInt() throws Exception {
        Method method = TestClass.class.getMethod("methodInt", int.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(I)");
    }

    @Test
    public void testGetMethodSignature_primitiveDouble() throws Exception {
        Method method = TestClass.class.getMethod("methodDouble", double.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(D)");
    }

    @Test
    public void testGetMethodSignature_primitiveBoolean() throws Exception {
        Method method = TestClass.class.getMethod("methodBoolean", boolean.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(Z)");
    }

    @Test
    public void testGetMethodSignature_primitiveLong() throws Exception {
        Method method = TestClass.class.getMethod("methodLong", long.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(J)");
    }

    @Test
    public void testGetMethodSignature_primitiveFloat() throws Exception {
        Method method = TestClass.class.getMethod("methodFloat", float.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(F)");
    }

    @Test
    public void testGetMethodSignature_primitiveByte() throws Exception {
        Method method = TestClass.class.getMethod("methodByte", byte.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(B)");
    }

    @Test
    public void testGetMethodSignature_primitiveChar() throws Exception {
        Method method = TestClass.class.getMethod("methodChar", char.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(C)");
    }

    @Test
    public void testGetMethodSignature_primitiveShort() throws Exception {
        Method method = TestClass.class.getMethod("methodShort", short.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(S)");
    }

    @Test
    public void testGetMethodSignature_multiplePrimitives() throws Exception {
        Method method = TestClass.class.getMethod("methodMultiplePrimitives", int.class, double.class, boolean.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(IDZ)");
    }

    // ========== Test Signature Generation for Objects ==========

    @Test
    public void testGetMethodSignature_string() throws Exception {
        Method method = TestClass.class.getMethod("methodString", String.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(Ljava/lang/String;)");
    }

    @Test
    public void testGetMethodSignature_object() throws Exception {
        Method method = TestClass.class.getMethod("methodObject", Object.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(Ljava/lang/Object;)");
    }

    @Test
    public void testGetMethodSignature_mixedPrimitiveAndObject() throws Exception {
        Method method = TestClass.class.getMethod("methodMixed", String.class, int.class, boolean.class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(Ljava/lang/String;IZ)");
    }

    // ========== Test Signature Generation for Arrays ==========

    @Test
    public void testGetMethodSignature_intArray() throws Exception {
        Method method = TestClass.class.getMethod("methodIntArray", int[].class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "([I)");
    }

    @Test
    public void testGetMethodSignature_stringArray() throws Exception {
        Method method = TestClass.class.getMethod("methodStringArray", String[].class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "([Ljava/lang/String;)");
    }

    @Test
    public void testGetMethodSignature_nestedIntArray() throws Exception {
        Method method = TestClass.class.getMethod("methodNestedIntArray", int[][].class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "([[I)");
    }

    @Test
    public void testGetMethodSignature_nestedStringArray() throws Exception {
        Method method = TestClass.class.getMethod("methodNestedStringArray", String[][].class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "([[Ljava/lang/String;)");
    }

    @Test
    public void testGetMethodSignature_tripleNestedArray() throws Exception {
        Method method = TestClass.class.getMethod("methodTripleNestedArray", int[][][].class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "([[[I)");
    }

    // ========== Test Edge Cases ==========

    @Test
    public void testGetMethodSignature_noParameters() throws Exception {
        Method method = TestClass.class.getMethod("methodNoParams");
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "()");
    }

    @Test
    public void testGetMethodSignature_varargs() throws Exception {
        Method method = TestClass.class.getMethod("methodVarargs", int[].class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        // Varargs are treated as arrays
        assertEquals(signature, "([I)");
    }

    @Test
    public void testGetMethodSignature_complexMix() throws Exception {
        Method method = TestClass.class.getMethod("methodComplex",
                String.class, int[].class, double.class, String[][].class);
        String signature = TypeSignatureUtil.getMethodSignature(method);
        assertEquals(signature, "(Ljava/lang/String;[ID[[Ljava/lang/String;)");
    }

    // ========== Test Finding Methods by Signature ==========

    @Test
    public void testFindMethodBySignature_intOverload() throws Exception {
        Method method = TypeSignatureUtil.findMethodBySignature(
                OverloadedClass.class, "add", "(II)");
        assertNotNull(method);
        assertEquals(method.getName(), "add");
        assertEquals(method.getParameterTypes().length, 2);
        assertEquals(method.getParameterTypes()[0], int.class);
        assertEquals(method.getParameterTypes()[1], int.class);
    }

    @Test
    public void testFindMethodBySignature_doubleOverload() throws Exception {
        Method method = TypeSignatureUtil.findMethodBySignature(
                OverloadedClass.class, "add", "(DD)");
        assertNotNull(method);
        assertEquals(method.getName(), "add");
        assertEquals(method.getParameterTypes().length, 2);
        assertEquals(method.getParameterTypes()[0], double.class);
        assertEquals(method.getParameterTypes()[1], double.class);
    }

    @Test
    public void testFindMethodBySignature_stringOverload() throws Exception {
        Method method = TypeSignatureUtil.findMethodBySignature(
                OverloadedClass.class, "add", "(Ljava/lang/String;Ljava/lang/String;)");
        assertNotNull(method);
        assertEquals(method.getName(), "add");
        assertEquals(method.getParameterTypes().length, 2);
        assertEquals(method.getParameterTypes()[0], String.class);
        assertEquals(method.getParameterTypes()[1], String.class);
    }

    @Test
    public void testFindMethodBySignature_mixedOverload() throws Exception {
        Method method = TypeSignatureUtil.findMethodBySignature(
                OverloadedClass.class, "process", "(Ljava/lang/String;I)");
        assertNotNull(method);
        assertEquals(method.getName(), "process");
        assertEquals(method.getParameterTypes().length, 2);
        assertEquals(method.getParameterTypes()[0], String.class);
        assertEquals(method.getParameterTypes()[1], int.class);
    }

    @Test(expectedExceptions = NoSuchMethodException.class)
    public void testFindMethodBySignature_notFound() throws Exception {
        TypeSignatureUtil.findMethodBySignature(
                OverloadedClass.class, "add", "(FFF)");
    }

    @Test(expectedExceptions = NoSuchMethodException.class)
    public void testFindMethodBySignature_wrongMethodName() throws Exception {
        TypeSignatureUtil.findMethodBySignature(
                OverloadedClass.class, "nonExistent", "(II)");
    }

    // ========== Test Type Descriptor ==========

    @Test
    public void testGetTypeDescriptor_primitives() {
        assertEquals(TypeSignatureUtil.getTypeDescriptor(int.class), "I");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(double.class), "D");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(boolean.class), "Z");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(long.class), "J");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(float.class), "F");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(byte.class), "B");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(char.class), "C");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(short.class), "S");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(void.class), "V");
    }

    @Test
    public void testGetTypeDescriptor_objects() {
        assertEquals(TypeSignatureUtil.getTypeDescriptor(String.class), "Ljava/lang/String;");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(Object.class), "Ljava/lang/Object;");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(Integer.class), "Ljava/lang/Integer;");
    }

    @Test
    public void testGetTypeDescriptor_arrays() {
        assertEquals(TypeSignatureUtil.getTypeDescriptor(int[].class), "[I");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(String[].class), "[Ljava/lang/String;");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(int[][].class), "[[I");
        assertEquals(TypeSignatureUtil.getTypeDescriptor(Object[][][].class), "[[[Ljava/lang/Object;");
    }

    // ========== Test Parse Parameter Types ==========

    @Test
    public void testParseParameterTypes_primitives() throws Exception {
        Class<?>[] types = TypeSignatureUtil.parseParameterTypes("(II)");
        assertEquals(types.length, 2);
        assertEquals(types[0], int.class);
        assertEquals(types[1], int.class);
    }

    @Test
    public void testParseParameterTypes_objects() throws Exception {
        Class<?>[] types = TypeSignatureUtil.parseParameterTypes("(Ljava/lang/String;Ljava/lang/Object;)");
        assertEquals(types.length, 2);
        assertEquals(types[0], String.class);
        assertEquals(types[1], Object.class);
    }

    @Test
    public void testParseParameterTypes_arrays() throws Exception {
        Class<?>[] types = TypeSignatureUtil.parseParameterTypes("([I[Ljava/lang/String;)");
        assertEquals(types.length, 2);
        assertEquals(types[0], int[].class);
        assertEquals(types[1], String[].class);
    }

    @Test
    public void testParseParameterTypes_mixed() throws Exception {
        Class<?>[] types = TypeSignatureUtil.parseParameterTypes("(ILjava/lang/String;D[I)");
        assertEquals(types.length, 4);
        assertEquals(types[0], int.class);
        assertEquals(types[1], String.class);
        assertEquals(types[2], double.class);
        assertEquals(types[3], int[].class);
    }

    @Test
    public void testParseParameterTypes_noParams() throws Exception {
        Class<?>[] types = TypeSignatureUtil.parseParameterTypes("()");
        assertEquals(types.length, 0);
    }

    @Test
    public void testParseParameterTypes_nestedArrays() throws Exception {
        Class<?>[] types = TypeSignatureUtil.parseParameterTypes("([[I[[Ljava/lang/String;)");
        assertEquals(types.length, 2);
        assertEquals(types[0], int[][].class);
        assertEquals(types[1], String[][].class);
    }

    // ========== Test Null Handling ==========

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetMethodSignature_nullMethod() {
        TypeSignatureUtil.getMethodSignature(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testFindMethodBySignature_nullClass() throws Exception {
        TypeSignatureUtil.findMethodBySignature(null, "add", "(II)");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testFindMethodBySignature_nullMethodName() throws Exception {
        TypeSignatureUtil.findMethodBySignature(TestClass.class, null, "(II)");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testFindMethodBySignature_nullSignature() throws Exception {
        TypeSignatureUtil.findMethodBySignature(TestClass.class, "add", null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetTypeDescriptor_null() {
        TypeSignatureUtil.getTypeDescriptor(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseParameterTypes_null() throws Exception {
        TypeSignatureUtil.parseParameterTypes(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseParameterTypes_empty() throws Exception {
        TypeSignatureUtil.parseParameterTypes("");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseParameterTypes_invalidFormat() throws Exception {
        TypeSignatureUtil.parseParameterTypes("II"); // Missing parentheses
    }

    // ========== Test Helper Classes ==========

    /**
     * Test class with various method signatures for testing.
     */
    public static class TestClass {
        public void methodInt(int x) {}
        public void methodDouble(double x) {}
        public void methodBoolean(boolean x) {}
        public void methodLong(long x) {}
        public void methodFloat(float x) {}
        public void methodByte(byte x) {}
        public void methodChar(char x) {}
        public void methodShort(short x) {}
        public void methodMultiplePrimitives(int a, double b, boolean c) {}
        public void methodString(String s) {}
        public void methodObject(Object o) {}
        public void methodMixed(String s, int i, boolean b) {}
        public void methodIntArray(int[] arr) {}
        public void methodStringArray(String[] arr) {}
        public void methodNestedIntArray(int[][] arr) {}
        public void methodNestedStringArray(String[][] arr) {}
        public void methodTripleNestedArray(int[][][] arr) {}
        public void methodNoParams() {}
        public void methodVarargs(int... args) {}
        public void methodComplex(String s, int[] arr1, double d, String[][] arr2) {}
    }

    /**
     * Test class with overloaded methods.
     */
    public static class OverloadedClass {
        public int add(int a, int b) { return a + b; }
        public double add(double a, double b) { return a + b; }
        public String add(String a, String b) { return a + b; }
        public void process(String s, int i) {}
        public void process(int i, String s) {}
        public void process(String s) {}
    }
}
