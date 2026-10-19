package org.labs.generator;

import java.util.random.RandomGenerator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GeneratorStringTest {

    private final Generator generator = new Generator();

    @ParameterizedTest
    @CsvSource({
        "0.0, 1",
        "0.05, 1",
        "0.1, 1",
        "0.11, 1",
        "1.0, 9",
        "1.25, 12",
        "9.9, 98",
        "9.99, 98",
        "10.0, 99",
        "100.0, 100"
    })
    void generatesStringsWithClampedLengths(double exponential, int expectedLength) throws Exception {
        var field = Generator.class.getDeclaredField("alphabet");
        field.setAccessible(true);
        String alphabet = (String) field.get(null);
        assertFalse(alphabet.isEmpty());

        var rnd = new RandomGenerator() {
            int exponentialCalls;
            int characterCalls;

            @Override
            public long nextLong() {
                throw new AssertionError("Unexpected random call");
            }

            @Override
            public double nextExponential() {
                exponentialCalls++;
                return exponential;
            }

            @Override
            public int nextInt(int bound) {
                assertEquals(alphabet.length(), bound);
                return characterCalls++ % bound;
            }
        };

        String actual = assertInstanceOf(String.class, generator.generateValueOfType(String.class, rnd));

        assertEquals(expectedLength, actual.length());
        assertEquals(1, rnd.exponentialCalls);
        assertEquals(expectedLength, rnd.characterCalls);
        for (int index = 0; index < expectedLength; index++) {
            assertEquals(alphabet.charAt(index % alphabet.length()), actual.charAt(index));
        }
    }
}
