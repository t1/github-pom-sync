package com.github.t1.github.pom.sync;

import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.NestedParameter;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.t1.github.pom.sync.PullRequests.PullRequestState.OPEN;
import static com.github.t1.github.pom.sync.PullRequests.RepositoryAffiliation.OWNER;
import static io.smallrye.graphql.client.typesafe.api.AuthorizationHeader.Type.BEARER;

@Slf4j
@Name("Repository")
public class PullRequests {
    private static final GitHubApi gitHubApi = TypesafeGraphQLClientBuilder.newBuilder()
            .endpoint("https://api.github.com/graphql")
            .build(GitHubApi.class);

    public void run(String login) {
        log.info("list all open pull requests for all repositories of {}", login);
        var start = Instant.now();
        PullRequests.openPullRequestsForRepositoriesOfOwner(login)
                .filter(repository -> repository.pullRequests.totalCount > 0)
                .map(repository -> repository.name + ": " + repository.pullRequests.totalCount)
                .forEach(log::info);
        log.debug("done in {} ms", Duration.between(start, Instant.now()).toMillis());
    }

    private static Stream<Repository> openPullRequestsForRepositoriesOfOwner(String login) {
        String cursor = null;
        var list = new ArrayList<Repository>();
        while (true) {
            var next = listRepositoriesWithPagination(login, cursor);
            list.addAll(next.nodes);
            if (!next.pageInfo.hasNextPage) break;
            cursor = next.pageInfo.endCursor;
        }
        return list.stream();
    }

    private static RepositoryConnection listRepositoriesWithPagination(String login, String cursor) {
        log.debug("list repositories with pagination, cursor: {}", cursor);
        return gitHubApi.repositoryOwner(login,
                        10, cursor, List.of(OWNER), false,
                        List.of(OPEN))
                .repositories;
    }

    @AuthorizationHeader(type = BEARER, confPrefix = "*")
    @GraphQLClientApi
    interface GitHubApi {
        @Query("repositoryOwner")
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
        int totalCount;
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
