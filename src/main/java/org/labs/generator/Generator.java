package org.labs.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

public class Generator {

    private static final Set<Class<?>> WRAPPERS = Set.of(
        Boolean.class, Byte.class, Short.class, Integer.class,
        Long.class, Float.class, Double.class, Character.class,
        Void.class
    );

    private static final Map<Class<?>, MinMax> BOUNDARIES = Map.ofEntries(
        Map.entry(long.class, new MinMax(Long.MIN_VALUE, Long.MAX_VALUE)),
        Map.entry(Long.class, new MinMax(Long.MIN_VALUE, Long.MAX_VALUE)),
        Map.entry(int.class, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE)),
        Map.entry(Integer.class, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE)),
        Map.entry(short.class, new MinMax(Short.MIN_VALUE, Short.MAX_VALUE)),
        Map.entry(Short.class, new MinMax(Short.MIN_VALUE, Short.MAX_VALUE)),
        Map.entry(byte.class, new MinMax(Byte.MIN_VALUE, Byte.MAX_VALUE)),
        Map.entry(Byte.class, new MinMax(Byte.MIN_VALUE, Byte.MAX_VALUE))
    );

    private static final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public Object generateValueOfType(Class<?> clazz) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return generateValueOfType(clazz, new Random());
    }

    public Object generateValueOfType(Class<?> clazz, RandomGenerator rnd) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        if (clazz.isPrimitive() || WRAPPERS.contains(clazz)) {
            return generatePrimitive(clazz, rnd);
        } else if (clazz == String.class) {

        } else if (clazz.isEnum()) {

        } else if (clazz.isArray()) {

        } else if (List.class.isAssignableFrom(clazz)) {

        } else if (Map.class.isAssignableFrom(clazz)) {

        } else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {

        } else if (clazz.getDeclaredConstructors().length != 0) {
            
        } else {
            throw new IllegalArgumentException("Unsupported class " + clazz);
        }
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        int randomConstructorIndex = rnd.nextInt(constructors.length);
        Constructor<?> randomConstructor = constructors[randomConstructorIndex];
        return randomConstructor.newInstance(111);
    }

    private Object generatePrimitive(Class<?> clazz, RandomGenerator rnd) {
        var mm = Optional.ofNullable(clazz.getAnnotation(Generatable.class))
            .map(a -> new MinMax(a.min(), a.max()))
            .orElse(new MinMax(Generatable.DEFAULT_MIN, Generatable.DEFAULT_MAX));

        if (clazz == void.class || clazz == Void.class) {
            return null;
        }

        do {
            var typeBoundaries = BOUNDARIES.get(clazz);
            if (typeBoundaries == null) {
                break;
            }
            long min = Math.min(typeBoundaries.max, Math.max(typeBoundaries.min, mm.min));
            long max = Math.min(typeBoundaries.max, Math.max(typeBoundaries.min, mm.max));
            var generated = Long.valueOf(nextLongInclusive(min, max, rnd));
            if (clazz == long.class || clazz == Long.class) {
                return generated;
            } else if (clazz == int.class || clazz == Integer.class) {
                return generated.intValue();
            } else if (clazz == short.class || clazz == Short.class) {
                return generated.shortValue();
            } else { // byte
                return generated.byteValue();
            }
        } while(false);
        
        if (clazz == float.class || clazz == Float.class) {
            return Float.valueOf(rnd.nextFloat((float) mm.min, (float) mm.max + 1.0f));
        }
        if (clazz == double.class || clazz == Double.class) {
            return Double.valueOf(rnd.nextDouble((double) mm.min, (double) mm.max + 1.0d));
        }

        if (clazz == boolean.class || clazz == Boolean.class) {
            return Boolean.valueOf(rnd.nextBoolean());
        }

        if (clazz == char.class || clazz == Character.class) {
            return Character.valueOf(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }
        
        throw new IllegalArgumentException("Class " + clazz + " is not primitive");
    }

    // Спасибо разрабам Java за то, что у RandomGenerator.nextLong(origin, bound) правая граница невключительная, так что можно попасть на переполнение с MAX_VALUE + 1
    private static long nextLongInclusive(long min, long max, RandomGenerator rnd) {
        if (max < min) {
            throw new IllegalArgumentException("max < min");
        } else if (max < Long.MAX_VALUE) {
            return rnd.nextLong(min, max + 1);
        } else if (min > Long.MIN_VALUE) {
            return rnd.nextLong(min - 1, max) + 1;
        } else {
            return rnd.nextLong();
        }
    }

    private static record MinMax(long min, long max) {}

}
