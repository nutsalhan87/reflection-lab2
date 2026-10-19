package org.labs.generator;

import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GeneratorAnnotatedElementTest {

    private final Generator generator = new Generator();

    @Test
    void generatesDefaultValueForClassElement() throws Exception {
        Object actual = generator.generateValueOfType(Integer.class, new StubRandom() {
            @Override
            public long nextLong(long origin, long bound) {
                assertEquals(Constants.MIN, origin);
                assertEquals(Constants.MAX + 1L, bound);
                return 123;
            }
        });

        assertEquals(123, actual);
    }

    @Test
    void generatesValueWithinParameterBounds() throws Exception {
        var parameter = Declarations.class.getDeclaredMethod("takes", int.class).getParameters()[0];

        Object actual = generator.generateValueOfType(parameter, new StubRandom() {
            @Override
            public long nextLong(long origin, long bound) {
                assertEquals(4, origin);
                assertEquals(5, bound);
                return 4;
            }
        });

        assertEquals(4, actual);
    }

    @Test
    void generatesArrayUsingAnnotationsOnFieldAndComponent() throws Exception {
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

        int[] actual = assertInstanceOf(int[].class, generator.generateValueOfType(field, rnd));

        assertArrayEquals(new int[] {7, 8, 9}, actual);
        assertEquals(1, rnd.exponentialCalls);
        assertEquals(3, rnd.valueCalls);
    }

    private static class Declarations {
        private @Generatable(min = 7, max = 9) int @Generatable(minLength = 3, maxLength = 3) [] values;

        private void takes(@Generatable(min = 4, max = 4) int value) {}
    }

    private abstract static class StubRandom implements RandomGenerator {
        @Override
        public long nextLong() {
            throw new AssertionError("Unexpected random call");
        }
    }
}
