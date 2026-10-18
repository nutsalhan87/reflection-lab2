package org.labs.generator;

import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GeneratorEnumTest {

    private final Generator generator = new Generator();

    @ParameterizedTest
    @ValueSource(classes = {Multiple.class, Single.class})
    void generatesEveryEnumConstantAtSelectedIndex(Class<?> type) throws Exception {
        Object[] constants = type.getEnumConstants();
        for (int index = 0; index < constants.length; index++) {
            int expectedIndex = index;
            var rnd = new StubRandom() {
                int calls;

                @Override
                public int nextInt(int bound) {
                    calls++;
                    assertEquals(constants.length, bound);
                    return expectedIndex;
                }
            };

            assertSame(constants[index], generator.generateValueOfType(type, rnd));
            assertEquals(1, rnd.calls);
        }
    }

    @Test
    void generatesNullForEmptyEnumWithoutUsingRandom() throws Exception {
        assertNull(generator.generateValueOfType(Empty.class, new StubRandom() {}));
    }

    private enum Multiple {
        FIRST, MIDDLE, LAST
    }

    private enum Single {
        ONLY
    }

    private enum Empty {}

    private abstract static class StubRandom implements RandomGenerator {
        @Override
        public long nextLong() {
            throw new AssertionError("Unexpected random call");
        }
    }
}
