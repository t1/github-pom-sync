package com.github.t1.github.pom.sync;

public class RunSync {
    public static void main(String... args) {
        Check.fail = message -> {
            System.err.println(message);
            System.exit(1);
        };
        new Sync().run();
        System.exit(0);
    }
}
