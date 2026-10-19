package org.labs.generator;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
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

    public Object generateValueOfType(AnnotatedElement element) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return generateValueOfType(element, new Random());
    }

    public Object generateValueOfType(AnnotatedElement element, RandomGenerator rnd) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return switch (element) {
            case Class<?> clazz -> generate(clazz, null, rnd);
            case Field field -> generate(field.getType(), field.getAnnotatedType(), rnd);
            case Parameter parameter -> generate(parameter.getType(), parameter.getAnnotatedType(), rnd);
            default -> throw new IllegalArgumentException(element.getClass() + " is not supported for generating random value");
        };
    }

    private Object generate(Class<?> clazz, AnnotatedType annotatedType, RandomGenerator rnd) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Generatable ann = Optional.ofNullable(annotatedType)
            .map(a -> a.getAnnotation(Generatable.class))
            .orElse(null);
        
        if (clazz.isPrimitive() || WRAPPERS.contains(clazz)) {
            return generatePrimitive(clazz, ann, rnd);
        } else if (clazz == String.class) {
            return generateString(ann, rnd);
        } else if (clazz.isEnum()) {
            return generateEnum(clazz, rnd);
        } else if (clazz.isArray()) {
            // TODO
        } else if (List.class.isAssignableFrom(clazz)) {
            // TODO
        } else if (Map.class.isAssignableFrom(clazz)) {
            // TODO
        } else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            // TODO
        } else if (clazz.getDeclaredConstructors().length != 0) {
            // TODO            
        } else {
            throw new IllegalArgumentException("Unsupported class " + clazz);
        }
        return null; // unreachable
    }

    private Object generatePrimitive(Class<?> clazz, Generatable ann, RandomGenerator rnd) {
        long min = ann != null ? ann.min() : Constants.MIN;
        long max = ann != null ? ann.max() : Constants.MAX;
        if (min > max) {
            throw new IllegalArgumentException("min > max for " + clazz);
        }
        
        if (clazz == void.class || clazz == Void.class) {
            return null;
        }

        do {
            var typeBoundaries = BOUNDARIES.get(clazz);
            if (typeBoundaries == null) {
                break;
            }
            min = Math.min(typeBoundaries.max, Math.max(typeBoundaries.min, min));
            max = Math.min(typeBoundaries.max, Math.max(typeBoundaries.min, max));
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
            return Float.valueOf(rnd.nextFloat((float) min, (float) max + 1.0f));
        }
        if (clazz == double.class || clazz == Double.class) {
            return Double.valueOf(rnd.nextDouble((double) min, (double) max + 1.0d));
        }

        if (clazz == boolean.class || clazz == Boolean.class) {
            return Boolean.valueOf(rnd.nextBoolean());
        }

        if (clazz == char.class || clazz == Character.class) {
            return Character.valueOf(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }
        
        throw new IllegalArgumentException("Class " + clazz + " is not primitive");
    }

    private Object generateString(Generatable ann, RandomGenerator rnd) {
        int minLength = ann != null ? ann.minLength() : Constants.MIN_LENGTH;
        int maxLength = ann != null ? ann.maxLength() : Constants.DEFAULT_LENGTH_BOUND;
        if (minLength > maxLength) {
            throw new IllegalArgumentException("min > max for length for String with annotation " + (ann == null ? "Generatable=null" : ann));
        }
        var length = (int) Math.min(Math.max(rnd.nextExponential() * (maxLength - minLength) * 0.1, minLength), maxLength);

        var strBuilder = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            strBuilder.append(generatePrimitive(Character.class, null, rnd));
        }

        return new String(strBuilder);
    }

    private Object generateEnum(Class<?> clazz, RandomGenerator rnd) {
        var consts = clazz.getEnumConstants();
        if (consts.length == 0) {
            return null;
        }
        return consts[rnd.nextInt(consts.length)];
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
