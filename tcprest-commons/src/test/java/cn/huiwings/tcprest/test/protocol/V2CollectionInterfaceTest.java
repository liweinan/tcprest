package cn.huiwings.tcprest.test.protocol;

import cn.huiwings.tcprest.converter.v2.ProtocolV2Converter;
import cn.huiwings.tcprest.extractor.v2.ProtocolV2Extractor;
import cn.huiwings.tcprest.server.Context;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.testng.Assert.*;

/**
 * Test V2 protocol support for common collection interfaces (List, Map, Set, etc.).
 *
 * <p>Verifies that collection interfaces can be used as method parameters without
 * requiring custom mappers. The actual implementations (ArrayList, HashMap, etc.)
 * are automatically serialized/deserialized.</p>
 *
 * @author Weinan Li
 * @created_at 2026-02-19
 */
public class V2CollectionInterfaceTest {

    /**
     * Test service with collection interface parameters.
     */
    public static class CollectionService {

        /**
         * Method with List parameter (interface, not concrete class).
         */
        public String processList(List<String> items) {
            if (items == null) return "null";
            return "list:" + items.size() + ":" + String.join(",", items);
        }

        /**
         * Method with Map parameter (interface, not concrete class).
         */
        public String processMap(Map<String, Integer> data) {
            if (data == null) return "null";
            return "map:" + data.size() + ":" + data.get("key1");
        }

        /**
         * Method with Set parameter (interface, not concrete class).
         */
        public String processSet(Set<String> items) {
            if (items == null) return "null";
            List<String> sorted = new ArrayList<>(items);
            Collections.sort(sorted);
            return "set:" + items.size() + ":" + String.join(",", sorted);
        }

        /**
         * Method returning List (deserialized as List<Object> without generic info).
         */
        public List<String> getList() {
            List<String> result = new ArrayList<>();
            result.add("a");
            result.add("b");
            result.add("c");
            return result;
        }

        /**
         * Method returning Map.
         */
        public Map<String, Integer> getMap() {
            Map<String, Integer> result = new HashMap<>();
            result.put("x", 1);
            result.put("y", 2);
            return result;
        }
    }

    @Test
    public void testList_ArrayList() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = CollectionService.class.getMethod("processList", List.class);

        // Encode: ArrayList instance passed to List parameter
        List<String> input = new ArrayList<>();
        input.add("apple");
        input.add("banana");
        input.add("cherry");

        Object[] params = new Object[]{input};
        String encoded = converter.encode(CollectionService.class, method, params, null);

        System.out.println("List encoded: " + encoded);

        // Decode: Should deserialize back to ArrayList (or compatible List implementation)
        Context context = extractor.extract(encoded);

        assertNotNull(context.getParams()[0], "List parameter should not be null");
        assertTrue(context.getParams()[0] instanceof List, "Should be a List");

        @SuppressWarnings("unchecked")
        List<String> decoded = (List<String>) context.getParams()[0];
        assertEquals(decoded.size(), 3, "List should have 3 elements");
        assertTrue(decoded.contains("apple"), "List should contain 'apple'");
        assertTrue(decoded.contains("banana"), "List should contain 'banana'");
        assertTrue(decoded.contains("cherry"), "List should contain 'cherry'");
    }

    @Test
    public void testList_LinkedList() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = CollectionService.class.getMethod("processList", List.class);

        // Encode: LinkedList instance passed to List parameter
        List<String> input = new LinkedList<>();
        input.add("x");
        input.add("y");

        Object[] params = new Object[]{input};
        String encoded = converter.encode(CollectionService.class, method, params, null);

        // Decode: Should deserialize back to LinkedList
        Context context = extractor.extract(encoded);

        @SuppressWarnings("unchecked")
        List<String> decoded = (List<String>) context.getParams()[0];
        assertEquals(decoded.size(), 2);
        assertTrue(decoded instanceof LinkedList, "Should preserve LinkedList type");
    }

    @Test
    public void testMap_HashMap() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = CollectionService.class.getMethod("processMap", Map.class);

        // Encode: HashMap instance passed to Map parameter
        Map<String, Integer> input = new HashMap<>();
        input.put("key1", 100);
        input.put("key2", 200);

        Object[] params = new Object[]{input};
        String encoded = converter.encode(CollectionService.class, method, params, null);

        System.out.println("Map encoded: " + encoded);

        // Decode: Should deserialize back to HashMap
        Context context = extractor.extract(encoded);

        assertNotNull(context.getParams()[0], "Map parameter should not be null");
        assertTrue(context.getParams()[0] instanceof Map, "Should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Integer> decoded = (Map<String, Integer>) context.getParams()[0];
        assertEquals(decoded.size(), 2, "Map should have 2 entries");
        assertEquals(decoded.get("key1"), Integer.valueOf(100));
        assertEquals(decoded.get("key2"), Integer.valueOf(200));
    }

    @Test
    public void testSet_HashSet() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = CollectionService.class.getMethod("processSet", Set.class);

        // Encode: HashSet instance passed to Set parameter
        Set<String> input = new HashSet<>();
        input.add("alpha");
        input.add("beta");
        input.add("gamma");

        Object[] params = new Object[]{input};
        String encoded = converter.encode(CollectionService.class, method, params, null);

        System.out.println("Set encoded: " + encoded);

        // Decode: Should deserialize back to HashSet
        Context context = extractor.extract(encoded);

        assertNotNull(context.getParams()[0], "Set parameter should not be null");
        assertTrue(context.getParams()[0] instanceof Set, "Should be a Set");

        @SuppressWarnings("unchecked")
        Set<String> decoded = (Set<String>) context.getParams()[0];
        assertEquals(decoded.size(), 3, "Set should have 3 elements");
        assertTrue(decoded.contains("alpha"));
        assertTrue(decoded.contains("beta"));
        assertTrue(decoded.contains("gamma"));
    }

    @Test
    public void testList_withMixedTypes() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = CollectionService.class.getMethod("processList", List.class);

        // Encode: List with mixed types (String, Integer, Boolean)
        // Without generic info, deserialized as List<Object>
        @SuppressWarnings({"rawtypes", "unchecked"})
        List input = new ArrayList();
        input.add("text");
        input.add(42);
        input.add(true);

        Object[] params = new Object[]{input};
        String encoded = converter.encode(CollectionService.class, method, params, null);

        // Decode: Should preserve all types
        Context context = extractor.extract(encoded);

        @SuppressWarnings("unchecked")
        List<Object> decoded = (List<Object>) context.getParams()[0];
        assertEquals(decoded.size(), 3);
        assertEquals(decoded.get(0), "text");
        assertEquals(decoded.get(1), 42);
        assertEquals(decoded.get(2), true);
    }

    @Test
    public void testList_empty() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = CollectionService.class.getMethod("processList", List.class);

        // Encode: Empty list
        List<String> input = new ArrayList<>();

        Object[] params = new Object[]{input};
        String encoded = converter.encode(CollectionService.class, method, params, null);

        // Decode: Should be empty list (not null)
        Context context = extractor.extract(encoded);

        @SuppressWarnings("unchecked")
        List<String> decoded = (List<String>) context.getParams()[0];
        assertNotNull(decoded);
        assertEquals(decoded.size(), 0);
    }

    @Test
    public void testList_null() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = CollectionService.class.getMethod("processList", List.class);

        // Encode: null list
        Object[] params = new Object[]{null};
        String encoded = converter.encode(CollectionService.class, method, params, null);

        // Verify ~ marker is used
        assertTrue(encoded.contains("[~]"), "Should contain ~ marker for null: " + encoded);

        // Decode: Should be null
        Context context = extractor.extract(encoded);

        assertNull(context.getParams()[0], "List parameter should be null");
    }

    @Test
    public void testMap_TreeMap() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = CollectionService.class.getMethod("processMap", Map.class);

        // Encode: TreeMap instance (sorted map)
        Map<String, Integer> input = new TreeMap<>();
        input.put("z", 3);
        input.put("a", 1);
        input.put("m", 2);

        Object[] params = new Object[]{input};
        String encoded = converter.encode(CollectionService.class, method, params, null);

        // Decode: Should preserve TreeMap type and ordering
        Context context = extractor.extract(encoded);

        @SuppressWarnings("unchecked")
        Map<String, Integer> decoded = (Map<String, Integer>) context.getParams()[0];
        assertTrue(decoded instanceof TreeMap, "Should preserve TreeMap type");
        assertEquals(decoded.size(), 3);
    }

    @Test
    public void testSet_TreeSet() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = CollectionService.class.getMethod("processSet", Set.class);

        // Encode: TreeSet instance (sorted set)
        Set<String> input = new TreeSet<>();
        input.add("zebra");
        input.add("apple");
        input.add("mango");

        Object[] params = new Object[]{input};
        String encoded = converter.encode(CollectionService.class, method, params, null);

        // Decode: Should preserve TreeSet type
        Context context = extractor.extract(encoded);

        @SuppressWarnings("unchecked")
        Set<String> decoded = (Set<String>) context.getParams()[0];
        assertTrue(decoded instanceof TreeSet, "Should preserve TreeSet type");
        assertEquals(decoded.size(), 3);
    }
}
