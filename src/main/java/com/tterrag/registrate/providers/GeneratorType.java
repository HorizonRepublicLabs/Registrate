package com.tterrag.registrate.providers;

public interface GeneratorType<T> {

    static <T> GeneratorType<T> named(String name) {
        return new GeneratorType<>() {
            @Override
            public String toString() {
                return name;
            }
        };
    }
}
