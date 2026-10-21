package org.labs.generator;

import java.util.List;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GeneratorListTest {

    private final Generator generator = new Generator();

    @Test
    void generatesDefaultListForClassElement() throws Exception {
        var rnd = new StubRandom() {
            int exponentialCalls;

            @Override
            public double nextExponential() {
                exponentialCalls++;
                return 0;
            }
        };

        List<?> actual = assertInstanceOf(List.class, generator.generateValueOfType(List.class, rnd));

        assertEquals(1, actual.size());
        assertEquals(Object.class, actual.getFirst().getClass());
        assertEquals(1, rnd.exponentialCalls);
    }

    @Test
    void generatesListUsingAnnotationsOnFieldAndElement() throws Exception {
        var field = Declarations.class.getDeclaredField("values");
        var rnd = new StubRandom() {
            int exponentialCalls;
            int valueCalls;

            @Override
            public double nextExponential() {
                exponentialCalls++;
                return 0;
            }

            @Override
            public long nextLong(long origin, long bound) {
                assertEquals(7, origin);
                assertEquals(10, bound);
                return origin + valueCalls++;
            }
        };

        List<?> actual = assertInstanceOf(List.class, generator.generateValueOfType(field, rnd));

        assertEquals(List.of(7, 8, 9), actual);
        assertEquals(1, rnd.exponentialCalls);
        assertEquals(3, rnd.valueCalls);
    }

    private static class Declarations {
        private @Generatable(minLength = 3, maxLength = 3) List<@Generatable(min = 7, max = 9) Integer> values;
    }

    private abstract static class StubRandom implements RandomGenerator {
        @Override
        public long nextLong() {
            throw new AssertionError("Unexpected random call");
        }
    }
}
