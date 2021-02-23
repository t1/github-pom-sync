package com.github.t1.github.pom.sync;

import java.util.function.Consumer;

public class Check {
    static Consumer<String> fail = message -> { throw new RuntimeException(message);};

    static void check(boolean condition, String failureMessage) {
        if (!condition) fail.accept(failureMessage);
    }
}
