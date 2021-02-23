package com.github.t1.github.pom.sync;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static com.github.t1.github.pom.sync.Check.check;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Git {
    @SneakyThrows({IOException.class, InterruptedException.class})
    public static String getOriginGithubHttpsUri() {
        var process = new ProcessBuilder("git", "remote", "get-url", "origin").start();
        check(process.waitFor(1, SECONDS), "timeout while getting git remote url");
        check(process.exitValue() == 0, "can't get git remote url [" + process.exitValue() + "]: "
            + read(process.getErrorStream()));
        var stdout = read(process.getInputStream());
        check(stdout != null, "can't read git remote url");
        return stdout;
    }

    private static String read(InputStream inputStream) {
        try (var scanner = new Scanner(inputStream, UTF_8).useDelimiter("\\Z")) {
            return scanner.hasNext() ? scanner.next() : null;
        }
    }
}
