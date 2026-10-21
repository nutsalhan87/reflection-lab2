package org.labs.generator;

import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratorClassTest {

    private final Generator generator = new Generator();

    @Test
    void generatesClassUsingNoArgsConstructorWithoutRandom() throws Exception {
        assertInstanceOf(NoArgs.class, generator.generateValueOfType(NoArgs.class, new StubRandom() {}));
    }

    @Test
    void generatesClassUsingFieldElement() throws Exception {
        var field = Declarations.class.getDeclaredField("value");

        assertInstanceOf(NoArgs.class, generator.generateValueOfType(field, new StubRandom() {}));
    }

    @Test
    void generatesConstructorArguments() throws Exception {
        var rnd = new StubRandom() {
            int longCalls;
            int intCalls;
            int exponentialCalls;

            @Override
            public long nextLong(long origin, long bound) {
                longCalls++;
                assertEquals(Constants.MIN, origin);
                assertEquals(Constants.MAX + 1, bound);
                return 42;
            }

            @Override
            public int nextInt(int bound) {
                intCalls++;
                assertEquals(62, bound);
                return 5;
            }

            @Override
            public double nextExponential() {
                exponentialCalls++;
                return 0;
            }
        };

        WithArgs actual = assertInstanceOf(WithArgs.class, generator.generateValueOfType(WithArgs.class, rnd));

        assertEquals(42, actual.number);
        assertEquals("5", actual.text);
        assertEquals(1, rnd.longCalls);
        assertEquals(1, rnd.intCalls);
        assertEquals(1, rnd.exponentialCalls);
    }

    @Test
    void usesAnnotationsOnConstructorParameters() throws Exception {
        var rnd = new StubRandom() {
            @Override
            public long nextLong(long origin, long bound) {
                if (origin == 7) {
                    assertEquals(10, bound);
                    return 8;
                }

                assertEquals(11, origin);
                assertEquals(14, bound);
                return 12;
            }
        };

        AnnotatedArgs actual = assertInstanceOf(AnnotatedArgs.class, generator.generateValueOfType(AnnotatedArgs.class, rnd));

        assertEquals(8, actual.x);
        assertEquals(12, actual.y);
    }

    @Test
    void fallsBackToAnotherConstructorWhenOneThrows() throws Exception {
        var rnd = new LenientRandom();

        Failing actual = assertInstanceOf(Failing.class, generator.generateValueOfType(Failing.class, rnd));

        assertEquals("0", actual.which);
    }

    @Test
    void usesPrivateConstructors() throws Exception {
        WithPrivateCtor actual = assertInstanceOf(WithPrivateCtor.class,
            generator.generateValueOfType(WithPrivateCtor.class, new StubRandom() {}));

        assertEquals(-1, actual.number);
    }

    @Test
    void throwsWhenNoConstructorSucceeds() {
        var rnd = new LenientRandom();

        assertThrows(InstantiationException.class, () -> generator.generateValueOfType(AlwaysFailing.class, rnd));
    }

    @Test
    void generatesRecordsUsingCanonicalConstructor() throws Exception {
        var rnd = new StubRandom() {
            int longCalls;

            @Override
            public long nextLong(long origin, long bound) {
                return ++longCalls;
            }
        };

        Point actual = assertInstanceOf(Point.class, generator.generateValueOfType(Point.class, rnd));

        assertEquals(new Point(1, 2), actual);
    }

    static class NoArgs {
        NoArgs() {}
    }

    static class WithArgs {
        final int number;
        final String text;

        WithArgs(int number, String text) {
            this.number = number;
            this.text = text;
        }
    }

    static class AnnotatedArgs {
        final int x;
        final int y;

        AnnotatedArgs(@Generatable(min = 7, max = 9) int x, @Generatable(min = 11, max = 13) int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Failing {
        final String which;

        Failing(int unused) {
            throw new IllegalStateException("boom");
        }

        Failing(String which) {
            this.which = which;
        }
    }

    static class AlwaysFailing {
        AlwaysFailing(int unused) {
            throw new IllegalStateException("boom");
        }

        AlwaysFailing(String unused) {
            throw new IllegalStateException("boom");
        }
    }

    static class WithPrivateCtor {
        final int number;

        private WithPrivateCtor() {
            this.number = -1;
        }
    }

    record Point(int x, int y) {}

    private static class Declarations {
        private NoArgs value;
    }

    private static class LenientRandom extends StubRandom {
        @Override
        public long nextLong(long origin, long bound) {
            return origin;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public double nextExponential() {
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
