package org.labs.generator;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratorMapTest {

    private final Generator generator = new Generator();

    @Test
    void generatesDefaultMapForClassElement() throws Exception {
        var rnd = new StubRandom() {
            int exponentialCalls;

            @Override
            public double nextExponential() {
                exponentialCalls++;
                return 0;
            }
        };

        Map<?, ?> actual = assertInstanceOf(Map.class, generator.generateValueOfType(Map.class, rnd));

        assertEquals(HashMap.class, actual.getClass());
        assertEquals(1, actual.size());
        assertTrue(actual.containsKey(null));
        assertNull(actual.get(null));
        assertEquals(1, rnd.exponentialCalls);
    }

    @Test
    void usesConstructorsOfConcreteMapClasses() throws Exception {
        Map<?, ?> hashMap = assertInstanceOf(Map.class, generator.generateValueOfType(HashMap.class, new ZeroRandom()));
        Map<?, ?> linkedHashMap = assertInstanceOf(Map.class, generator.generateValueOfType(LinkedHashMap.class, new ZeroRandom()));

        assertEquals(HashMap.class, hashMap.getClass());
        assertEquals(LinkedHashMap.class, linkedHashMap.getClass());
    }

    @Test
    void generatesRawMapUsingObjectForKeysAndValues() throws Exception {
        var field = Declarations.class.getDeclaredField("rawValues");
        var rnd = new ZeroRandom();

        Map<?, ?> actual = assertInstanceOf(Map.class, generator.generateValueOfType(field, rnd));

        assertEquals(HashMap.class, actual.getClass());
        assertEquals(1, actual.size());
        assertTrue(actual.containsKey(null));
        assertNull(actual.get(null));
        assertEquals(1, rnd.exponentialCalls);
    }

    @Test
    void generatesWildcardMapUsingObjectForKeysAndValues() throws Exception {
        var field = Declarations.class.getDeclaredField("wildcardValues");
        var rnd = new ZeroRandom();

        Map<?, ?> actual = assertInstanceOf(Map.class, generator.generateValueOfType(field, rnd));

        assertEquals(HashMap.class, actual.getClass());
        assertEquals(1, actual.size());
        assertTrue(actual.containsKey(null));
        assertNull(actual.get(null));
        assertEquals(1, rnd.exponentialCalls);
    }

    @Test
    void generatesMapUsingDifferentKeyAndValueTypes() throws Exception {
        var field = Declarations.class.getDeclaredField("typedValues");
        var rnd = new StubRandom() {
            int exponentialCalls;
            int keyCalls;
            int characterCalls;

            @Override
            public double nextExponential() {
                exponentialCalls++;
                return 0;
            }

            @Override
            public long nextLong(long origin, long bound) {
                assertEquals(7, origin);
                assertEquals(10, bound);
                return origin + keyCalls++;
            }

            @Override
            public int nextInt(int bound) {
                assertEquals(62, bound);
                return characterCalls++;
            }
        };

        Map<?, ?> actual = assertInstanceOf(Map.class, generator.generateValueOfType(field, rnd));

        assertEquals(Map.of(7, "0", 8, "1", 9, "2"), actual);
        assertEquals(4, rnd.exponentialCalls);
        assertEquals(3, rnd.keyCalls);
        assertEquals(3, rnd.characterCalls);
    }

    @Test
    void generatesMapUsingAnnotationsOnFieldKeyAndValue() throws Exception {
        var field = Declarations.class.getDeclaredField("values");
        var rnd = new StubRandom() {
            int exponentialCalls;
            int keyCalls;
            int valueCalls;

            @Override
            public double nextExponential() {
                exponentialCalls++;
                return 0;
            }

            @Override
            public long nextLong(long origin, long bound) {
                if (origin == 7) {
                    assertEquals(10, bound);
                    return origin + keyCalls++;
                }

                assertEquals(11, origin);
                assertEquals(14, bound);
                return origin + valueCalls++;
            }
        };

        Map<?, ?> actual = assertInstanceOf(Map.class, generator.generateValueOfType(field, rnd));

        assertEquals(Map.of(7, 11, 8, 12, 9, 13), actual);
        assertEquals(1, rnd.exponentialCalls);
        assertEquals(3, rnd.keyCalls);
        assertEquals(3, rnd.valueCalls);
    }

    private static class Declarations {
        private @Generatable(minLength = 3, maxLength = 3)
        Map<@Generatable(min = 7, max = 9) Integer, @Generatable(min = 11, max = 13) Integer> values;

        private @Generatable(minLength = 3, maxLength = 3) Map rawValues;

        private @Generatable(minLength = 3, maxLength = 3) Map<?, ?> wildcardValues;

        private @Generatable(minLength = 3, maxLength = 3)
        Map<@Generatable(min = 7, max = 9) Integer, @Generatable(minLength = 1, maxLength = 1) String> typedValues;
    }

    private static class ZeroRandom extends StubRandom {
        int exponentialCalls;

        @Override
        public double nextExponential() {
            exponentialCalls++;
            return 0;
        }
    }

    private abstract static class StubRandom implements RandomGenerator {
        @Override
        public long nextLong() {
            throw new AssertionError("Unexpected random call");
        }
    }
}
