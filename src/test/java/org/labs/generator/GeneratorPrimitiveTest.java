package org.labs.generator;

import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class GeneratorPrimitiveTest {

    private final Generator generator = new Generator();

    @ParameterizedTest
    @MethodSource("integerTypes")
    void generatesIntegersWithinInclusiveBounds(Class<?> type, Class<?> wrapper, long min, long max) throws Exception {
        for (long expected : new long[] {min, 0, max}) {
            RandomGenerator rnd = new StubRandom() {
                @Override
                public long nextLong(long origin, long bound) {
                    assertEquals(min, origin);
                    assertEquals(max + 1, bound);
                    return expected;
                }
            };

            Object actual = generator.generateValueOfType(type, rnd);

            assertInstanceOf(wrapper, actual);
            assertEquals(expected, ((Number) actual).longValue());
        }
    }

    static Stream<Arguments> integerTypes() {
        return Stream.of(
            Arguments.of(byte.class, Byte.class, Byte.MIN_VALUE, Byte.MAX_VALUE),
            Arguments.of(Byte.class, Byte.class, Byte.MIN_VALUE, Byte.MAX_VALUE),
            Arguments.of(short.class, Short.class, Generatable.DEFAULT_MIN, Generatable.DEFAULT_MAX),
            Arguments.of(Short.class, Short.class, Generatable.DEFAULT_MIN, Generatable.DEFAULT_MAX),
            Arguments.of(int.class, Integer.class, Generatable.DEFAULT_MIN, Generatable.DEFAULT_MAX),
            Arguments.of(Integer.class, Integer.class, Generatable.DEFAULT_MIN, Generatable.DEFAULT_MAX),
            Arguments.of(long.class, Long.class, Generatable.DEFAULT_MIN, Generatable.DEFAULT_MAX),
            Arguments.of(Long.class, Long.class, Generatable.DEFAULT_MIN, Generatable.DEFAULT_MAX)
        );
    }

    @ParameterizedTest
    @ValueSource(classes = {float.class, Float.class})
    void generatesFloatsWithinConfiguredBounds(Class<?> type) throws Exception {
        float min = Generatable.DEFAULT_MIN;
        float bound = Generatable.DEFAULT_MAX + 1.0f;
        for (float expected : new float[] {min, 0.0f, Math.nextDown(bound)}) {
            RandomGenerator rnd = new StubRandom() {
                @Override
                public float nextFloat(float origin, float actualBound) {
                    assertEquals(min, origin);
                    assertEquals(bound, actualBound);
                    return expected;
                }
            };

            Object actual = generator.generateValueOfType(type, rnd);

            assertInstanceOf(Float.class, actual);
            assertEquals(expected, ((Float) actual).floatValue());
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {double.class, Double.class})
    void generatesDoublesWithinConfiguredBounds(Class<?> type) throws Exception {
        double min = Generatable.DEFAULT_MIN;
        double bound = Generatable.DEFAULT_MAX + 1.0d;
        for (double expected : new double[] {min, 0.0d, Math.nextDown(bound)}) {
            RandomGenerator rnd = new StubRandom() {
                @Override
                public double nextDouble(double origin, double actualBound) {
                    assertEquals(min, origin);
                    assertEquals(bound, actualBound);
                    return expected;
                }
            };

            Object actual = generator.generateValueOfType(type, rnd);

            assertInstanceOf(Double.class, actual);
            assertEquals(expected, ((Double) actual).doubleValue());
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {boolean.class, Boolean.class})
    void generatesBothBooleanValues(Class<?> type) throws Exception {
        for (boolean expected : new boolean[] {false, true}) {
            RandomGenerator rnd = new StubRandom() {
                @Override
                public boolean nextBoolean() {
                    return expected;
                }
            };

            assertEquals(expected, generator.generateValueOfType(type, rnd));
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {char.class, Character.class})
    void generatesEveryAlphabetCharacter(Class<?> type) throws Exception {
        var field = Generator.class.getDeclaredField("alphabet");
        field.setAccessible(true);
        String alphabet = (String) field.get(null);
        assertFalse(alphabet.isEmpty());

        for (int index = 0; index < alphabet.length(); index++) {
            int expectedIndex = index;
            RandomGenerator rnd = new StubRandom() {
                @Override
                public int nextInt(int bound) {
                    assertEquals(alphabet.length(), bound);
                    return expectedIndex;
                }
            };

            assertEquals(alphabet.charAt(index), generator.generateValueOfType(type, rnd));
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {void.class, Void.class})
    void generatesNullForVoidWithoutUsingRandom(Class<?> type) throws Exception {
        assertNull(generator.generateValueOfType(type, new StubRandom() {}));
    }

    private abstract static class StubRandom implements RandomGenerator {
        @Override
        public long nextLong() {
            throw new AssertionError("Unexpected random call");
        }
    }
}
