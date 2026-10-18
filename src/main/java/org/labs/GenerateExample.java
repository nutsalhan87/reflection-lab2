package org.labs;


import org.labs.classes.Example;
import org.labs.generator.Generator;

public class GenerateExample {
    public static void main(String[] args) {
        var gen = new Generator();
        try {
            Object generated = gen.generateValueOfType(Example.class);
            System.out.println(generated);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}