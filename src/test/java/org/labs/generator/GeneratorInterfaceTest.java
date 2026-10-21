package org.labs.generator;

import java.util.Set;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import org.labs.classes.Shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratorInterfaceTest {

    private final Generator generator = new Generator();

    @Test
    void generatesImplementorOfProductionInterface() throws Exception {
        Shape actual = assertInstanceOf(Shape.class, generator.generateValueOfType(Shape.class, new LenientRandom()));

        assertTrue(Set.of("Rectangle", "Triangle").contains(actual.getClass().getSimpleName()));
    }

    @Test
    void generatesImplementorUsingFieldElement() throws Exception {
        var field = Declarations.class.getDeclaredField("shape");

        assertInstanceOf(Shape.class, generator.generateValueOfType(field, new LenientRandom()));
    }

    @Test
    void generatesChildOfAbstractClass() throws Exception {
        Animal actual = assertInstanceOf(Animal.class, generator.generateValueOfType(Animal.class, new LenientRandom()));

        assertEquals(Cat.class, actual.getClass());
        assertEquals("0", actual.name);
    }

    @Test
    void picksImplementorByShufflingWithRandom() throws Exception {
        var rnd = new StubRandom() {
            int shuffleCalls;

            @Override
            public int nextInt(int bound) {
                shuffleCalls++;
                assertEquals(2, bound);
                return 0;
            }
        };

        Object actual = generator.generateValueOfType(Pet.class, rnd);

        assertTrue(actual instanceof Parrot || actual instanceof Hamster);
        assertEquals(1, rnd.shuffleCalls);
    }

    @Test
    void fallsBackToWorkingImplementorWhenOneFails() throws Exception {
        assertInstanceOf(Tasty.class, generator.generateValueOfType(Breakfast.class, new LenientRandom()));
    }

    @Test
    void throwsForInterfaceWithoutImplementors() {
        var rnd = new StubRandom() {};

        assertThrows(InstantiationException.class, () -> generator.generateValueOfType(Lonely.class, rnd));
    }

    @Test
    void throwsForAbstractClassWithoutChildren() {
        var rnd = new StubRandom() {};

        assertThrows(InstantiationException.class, () -> generator.generateValueOfType(Orphan.class, rnd));
    }

    @Test
    void throwsWhenAllImplementorsFail() {
        var rnd = new LenientRandom();

        assertThrows(InstantiationException.class, () -> generator.generateValueOfType(Hopeless.class, rnd));
    }

    static abstract class Animal {
        final String name;

        Animal(String name) {
            this.name = name;
        }
    }

    static final class Cat extends Animal {
        Cat(String name) {
            super(name);
        }
    }

    interface Pet {}

    static final class Parrot implements Pet {
        Parrot() {}
    }

    static final class Hamster implements Pet {
        Hamster() {}
    }

    interface Breakfast {}

    static final class Burnt implements Breakfast {
        Burnt() {
            throw new IllegalStateException("boom");
        }
    }

    static final class Tasty implements Breakfast {
        Tasty() {}
    }

    interface Lonely {}

    static abstract class Orphan {}

    interface Hopeless {}

    static final class Doomed implements Hopeless {
        Doomed() {
            throw new IllegalStateException("boom");
        }
    }

    private static class Declarations {
        private Shape shape;
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
        public double nextDouble(double origin, double bound) {
            return origin;
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
