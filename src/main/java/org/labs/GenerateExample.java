package org.labs;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.labs.classes.Shape;
import org.labs.generator.Generatable;
import org.labs.generator.Generator;

public class GenerateExample {

    enum Mood {
        HAPPY, NEUTRAL, GRUMPY
    }

    record Person(
        @Generatable(min = 18, max = 80) int age,
        @Generatable(minLength = 3, maxLength = 10) String name,
        Mood mood,
        @Generatable(minLength = 2, maxLength = 4) List<@Generatable(min = 0, max = 100) Integer> scores,
        @Generatable(minLength = 1, maxLength = 3)
        Map<@Generatable(minLength = 2, maxLength = 6) String, @Generatable(min = 0, max = 9) Integer> grades
    ) {}

    static class Fields {
        @Generatable(min = -10, max = 10) int smallInt;
        @Generatable(min = 1_000_000, max = 9_999_999) long longId;
        @Generatable(min = 0, max = 1) float ratio;
        @Generatable(min = 0, max = 100) double percentage;
        boolean flag;
        char letter;
        @Generatable(minLength = 8, maxLength = 8) String password;
        Mood mood;
        @Generatable(min = 1, max = 49) int @Generatable(minLength = 6, maxLength = 6) [] lotteryTicket;
        @Generatable(minLength = 2, maxLength = 5) List<@Generatable(minLength = 2, maxLength = 4) String> nicknames;
        @Generatable(minLength = 2, maxLength = 3)
        Map<@Generatable(minLength = 2, maxLength = 6) String, @Generatable(min = 0, max = 100) Integer> leaderboard;
        Shape shape;
        Person person;
    }

    public static void main(String[] args) throws Exception {
        var generator = new Generator();

        System.out.println("=== Generated values ===");
        for (Field field : Fields.class.getDeclaredFields()) {
            Object value = generator.generateValueOfType(field);
            System.out.printf("%-15s : %s%n", field.getName(), render(value));
        }
    }

    private static String render(Object value) {
        return switch (value) {
            case Shape s -> "%s[area=%.2f, perimeter=%.2f]".formatted(
                s.getClass().getSimpleName(), s.getArea(), s.getPerimeter());
            case int[] arr -> Arrays.toString(arr);
            case Object[] arr -> Arrays.deepToString(arr);
            case Object o -> String.valueOf(o);
        };
    }
}
