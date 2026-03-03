package com.github.t1.github.pom.sync;

import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.NestedParameter;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.t1.github.pom.sync.PullRequests.PullRequestState.OPEN;
import static com.github.t1.github.pom.sync.PullRequests.RepositoryAffiliation.OWNER;
import static io.smallrye.graphql.client.typesafe.api.AuthorizationHeader.Type.BEARER;

@Slf4j
public class PullRequests {
    public void run(String login) {
        log.info("list all open pull requests for all repositories of {}", login);
        var start = Instant.now();
        repositoriesOf(login)
                .filter(repo -> repo.pullRequests.totalCount > 0)
                .forEach(repo -> log.info("{}: {}", repo.name, repo.pullRequests.totalCount));
        log.debug("done in {} ms", Duration.between(start, Instant.now()).toMillis());
    }

    private static Stream<Repository> repositoriesOf(String login) {
        var pageSize = 100;
        // problem: `hasNextPage` is for the _next_ page, but `iterate` checks it _before_ it calls the `next` UnaryOperator
        // solution: use `null` as an end-indicator and produce that when the last `hasNextPage` has been consumed
        return Stream.iterate(next(pageSize, login, null),
                        Objects::nonNull,
                        connection -> connection.pageInfo.hasNextPage
                                ? next(pageSize, login, connection.pageInfo.endCursor)
                                : null)
                .flatMap(connection -> connection.nodes.stream());
    }

    private static RepositoryConnection next(int first, String login, String cursor) {
        log.debug("next {} repositories for {} with pagination, cursor: {}", first, login, cursor);
        return gitHubApi.repositoryOwner(
                login,
                first,
                cursor,
                List.of(OWNER),
                false,
                List.of(OPEN))
                .repositories;
    }

    private static final GitHubApi gitHubApi = TypesafeGraphQLClientBuilder.newBuilder()
            .endpoint("https://api.github.com/graphql")
            .build(GitHubApi.class);

    @AuthorizationHeader(type = BEARER, confPrefix = "*")
    interface GitHubApi {
        RepositoryOwner repositoryOwner(
                @NonNull String login,
                @NestedParameter("repositories") int first,
                @NestedParameter("repositories") String after,
                @NestedParameter("repositories") List<@NonNull RepositoryAffiliation> affiliations,
                @NestedParameter("repositories") Boolean isArchived,
                @NestedParameter("repositories.nodes.pullRequests") List<@NonNull PullRequestState> states);
    }

    static class RepositoryOwner {
        RepositoryConnection repositories;
    }

    enum RepositoryAffiliation {COLLABORATOR, ORGANIZATION_MEMBER, OWNER}

    static class RepositoryConnection {
        List<Repository> nodes;
        PageInfo pageInfo;
    }

    static class Repository {
        String name;

        @NonNull PullRequestConnection pullRequests;
    }

    enum PullRequestState {CLOSED, MERGED, OPEN}

    static class PullRequestConnection {
        @NonNull Integer totalCount;
    }

    static class PageInfo {
        @NonNull Boolean hasNextPage;
        String endCursor;
    }
}
