package org.labs.generator;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import org.reflections.Reflections;
import static org.reflections.scanners.Scanners.SubTypes;

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

    private static final AnnotatedType OBJECT_TYPE =
        ObjectHolder.class.getRecordComponents()[0].getAnnotatedType();

    private static final Reflections REFLECTIONS = new Reflections("org.labs");

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
            return generateArray(clazz, annotatedType, ann, rnd);
        } else if (List.class.isAssignableFrom(clazz)) {
            return generateList(clazz, annotatedType, ann, rnd);
        } else if (Map.class.isAssignableFrom(clazz)) {
            return generateMap(clazz, annotatedType, ann, rnd);
        } else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return generateInterfaceOrAbstract(clazz, annotatedType, rnd);
        } else if (clazz.getDeclaredConstructors().length != 0) {
            return generateClass(clazz, annotatedType, rnd);
        }
        throw new IllegalArgumentException("Unsupported class " + clazz);
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
            return Float.valueOf(rnd.nextFloat((float) min, (float) max));
        }
        if (clazz == double.class || clazz == Double.class) {
            return Double.valueOf(rnd.nextDouble((double) min, (double) max));
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
        var strBuilder = new StringBuilder();
        var length = getLength(ann, rnd);
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

    private Object generateArray(Class<?> clazz, AnnotatedType annotated, Generatable ann, RandomGenerator rnd) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        var componentClass = clazz.getComponentType();
        var componentType = annotated instanceof AnnotatedArrayType a ? a.getAnnotatedGenericComponentType() : null;
        
        var length = getLength(ann, rnd);
        Object array = Array.newInstance(componentClass, length);
        for (int i = 0; i < length; ++i) {
            Array.set(array, i, generate(componentClass, componentType, rnd));
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    private Object generateList(Class<?> clazz, AnnotatedType annotated, Generatable ann, RandomGenerator rnd) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        AnnotatedType genericType = switch(annotated) {
            case AnnotatedParameterizedType apt -> {
                var actualTypeArg = apt.getAnnotatedActualTypeArguments()[0];
                yield actualTypeArg instanceof AnnotatedWildcardType ? OBJECT_TYPE : actualTypeArg;
            }
            case AnnotatedType _ -> OBJECT_TYPE;
            case null -> OBJECT_TYPE;
        };

        List<Object> list;
        try {
            list = (List<Object>) clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException _) {
            list = new ArrayList<>();
        }
        
        var length = getLength(ann, rnd);
        for (int i = 0; i < length; ++i) {
            list.add(generate(toClass(genericType.getType()), genericType, rnd));
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private Object generateMap(Class<?> clazz, AnnotatedType annotated, Generatable ann, RandomGenerator rnd) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        AnnotatedType keyType, valType; 
        if (annotated instanceof AnnotatedParameterizedType apt) {
            keyType = apt.getAnnotatedActualTypeArguments()[0];
            keyType = keyType instanceof AnnotatedWildcardType ? OBJECT_TYPE : keyType;
            valType = apt.getAnnotatedActualTypeArguments()[1];
            valType = valType instanceof AnnotatedWildcardType ? OBJECT_TYPE : valType;
        } else {
            keyType = OBJECT_TYPE;
            valType = OBJECT_TYPE;
        }
        
        Map<Object, Object> map;
        try {
            map = (Map<Object, Object>) clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException _) {
            map = new HashMap<>();
        }
        
        var length = getLength(ann, rnd);
        for (int i = 0; i < length; ++i) {
            var key = generate(toClass(keyType.getType()), keyType, rnd); 
            var val = generate(toClass(valType.getType()), valType, rnd); 
            map.put(key, val);
        }

        return map;
    }

    private Object generateClass(Class<?> clazz, AnnotatedType annotated, RandomGenerator rnd) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        var randomIndexes = IntStream.range(0, clazz.getDeclaredConstructors().length).toArray();
        for (int i = randomIndexes.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int temp = randomIndexes[i];
            randomIndexes[i] = randomIndexes[j];
            randomIndexes[j] = temp;
        }
        
        for (var i : randomIndexes) {
            try {
                var constructor = clazz.getDeclaredConstructors()[i];
                constructor.setAccessible(true);
                var classes = constructor.getParameterTypes();
                var annotatedTypes = constructor.getAnnotatedParameterTypes();
                assert classes.length == annotatedTypes.length;
                var args = new Object[constructor.getParameterCount()];
                for (int j = 0; j < constructor.getParameterCount(); ++j) {
                    args[j] = generate(classes[j], annotatedTypes[j], rnd);
                }
                return constructor.newInstance(args);
            } catch (Throwable t) {}
        }

        throw new InstantiationException("Can't create instance of class " + clazz + " using any declared constructor");
    }

    private Object generateInterfaceOrAbstract(Class<?> clazz, AnnotatedType annotated, RandomGenerator rnd) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        var childrenAndImplementors = REFLECTIONS.get(SubTypes.get(clazz).asClass());
        if (childrenAndImplementors.isEmpty()) {
            if (clazz.isInterface()) {
                throw new InstantiationException("Can't create instance of interface " + clazz + ": no implementors found in current package");
            } else {
                throw new InstantiationException("Can't create instance of abstract class " + clazz + ": no children found in current package");
            }
        }

        var shuffled = new ArrayList<>(childrenAndImplementors);
        Collections.shuffle(shuffled, rnd);
        for (var childOrImplementor : shuffled) {
            try {
                return generate(childOrImplementor, annotated, rnd);
            } catch (Throwable t) {}
        }
        
        throw new InstantiationException("Can't create instance of " + clazz + ": no appropriate child or implementor found");
    }

    private static int getLength(Generatable ann, RandomGenerator rnd) {
        int minLength = ann != null ? Math.max(ann.minLength(), Constants.MIN_LENGTH) : Constants.MIN_LENGTH;
        int maxLength = ann != null ? Math.min(ann.maxLength(), Constants.MAX_LENGTH) : Constants.DEFAULT_LENGTH_BOUND;
        if (minLength > maxLength) {
            throw new IllegalArgumentException("min > max for length");
        }
        var mean = rnd.nextExponential() * (minLength + ((double) maxLength - minLength) * 0.1);
        return (int) Math.min(Math.max(mean, minLength), maxLength);
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

    private static Class<?> toClass(Type type) {
        return switch (type) {
            case Class<?> c -> c;
            case ParameterizedType pt -> (Class<?>) pt.getRawType();
            case GenericArrayType gat -> toClass(gat.getGenericComponentType()).arrayType();
            case TypeVariable<?> tv -> toClass(tv.getBounds()[0]);
            case WildcardType wt -> toClass(wt.getUpperBounds()[0]);
            default -> Object.class;
        };
    }

    private static record MinMax(long min, long max) {}

    private record ObjectHolder(Object value) {}

}
