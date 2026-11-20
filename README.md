# github-pom-sync

Experimental Java utilities around GitHub and Maven project maintenance.

## Sync POM metadata & prepare for Maven Central release

Turn an existing GitHub-based Maven project with a minimal `pom.xml` into one that is ready to be released to Maven Central.
- enriches a `pom.xml` with metadata fetched from GitHub
- removes obsolete JCenter configuration
- adds the Maven Central release setup a project typically needs

## List open PRs

Counts all open pull requests across your owned GitHub repositories.
