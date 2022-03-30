package com.github.t1.github.pom.sync;

import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import org.eclipse.microprofile.graphql.NonNull;

import java.util.List;
import java.util.regex.Pattern;

import static io.smallrye.graphql.client.typesafe.api.AuthorizationHeader.Type.BEARER;

class Repository {
    private static final GitHubApi gitHubApi = TypesafeGraphQLClientBuilder.newBuilder()
        .endpoint("https://api.github.com/graphql")
        .build(GitHubApi.class);

    public static Repository fetch(String origin) {
        var matcher = Pattern.compile("https://github.com/(?<owner>.+)/(?<name>.+)").matcher(origin);
        Check.check(matcher.matches(), "not a valid github remote origin: '" + origin + "'");
        var owner = matcher.group("owner");
        var name = matcher.group("name");
        if (name.endsWith(".git")) name = name.substring(0, name.length() - 4);

        return Repository.fetch(owner, name);
    }

    public static Repository fetch(String owner, String name) {
        var start = System.currentTimeMillis();
        try {
            return gitHubApi.repository(owner, name);
        } finally {
            System.err.println("GitHub call took " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    @AuthorizationHeader(type = BEARER, confPrefix = "*")
    interface GitHubApi {
        Repository repository(@NonNull String owner, @NonNull String name);
    }

    String url;
    String description;
    LicenseInfo licenseInfo;
    Collaborators collaborators;

    static class LicenseInfo {
        String name;
        String url;
    }

    static class Collaborators {
        List<Collaborator> nodes;
        int totalCount;
    }

    static class Collaborator {
        String login;
        String name;
    }
}
