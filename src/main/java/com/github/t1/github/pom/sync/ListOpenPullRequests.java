package com.github.t1.github.pom.sync;

public class ListOpenPullRequests {
    public static void main(String... args) {
        Check.fail = message -> {
            System.err.println(message);
            System.exit(1);
        };

        if (args.length != 1) {
            throw new IllegalArgumentException("need exactly one argument: <owner>");
        }
        try {
            new PullRequests().run(args[0]);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            System.exit(0); // why is this necessary?
        }
    }
}
