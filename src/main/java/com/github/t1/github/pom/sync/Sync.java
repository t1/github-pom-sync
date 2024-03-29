package com.github.t1.github.pom.sync;

import com.github.t1.xml.Xml;
import com.github.t1.xml.XmlElement;
import com.github.t1.xml.XmlElement.XmlPosition;

import java.nio.file.Path;

import static com.github.t1.xml.XmlElement.after;
import static com.github.t1.xml.XmlElement.atEnd;

public class Sync implements Runnable {
    private final String origin;
    private final Xml pom;
    private final Repository repository;

    private final XmlPosition furtherUp;
    private final XmlPosition furtherDown;

    public Sync() {
        this.pom = Xml.load(Path.of("pom.xml").toUri());
        this.origin = Git.getOriginGithubHttpsUri();
        this.repository = Repository.fetch(origin);

        this.furtherUp = pom.beforeExisting("properties")
            .or(() -> pom.afterExisting("description"))
            .or(() -> pom.afterExisting("name"))
            .or(() -> pom.afterExisting("packaging"))
            .orElse(after("version"));
        this.furtherDown = pom.beforeExisting("build")
            .or(() -> pom.beforeExisting("profiles"))
            .orElse(atEnd());
    }

    @Override public void run() {
        apply();
        pom.save();
    }

    private boolean hasPlugin(String artifactId) {
        var found = pom.find("//plugins/plugin/artifactId[text()='" + artifactId + "']");
        return !found.isEmpty();
    }

    private boolean hasProfile(@SuppressWarnings("SameParameterValue") String profileId) {
        var found = pom.find("profiles/profile/id[text()='" + profileId + "']");
        return !found.isEmpty();
    }

    private void apply() {
        removeJCenter();

        applyName();
        applyDescription();
        applyUrl();
        applyScm();
        applyLicense();
        applyDistributionManagement();
        applyDevelopers();

        applyPlugins();

        applyReleaseProfiles();
    }

    private void removeJCenter() {
        var repositories = pom.getOptionalElement("repositories");
        if (repositories.isEmpty()) return;
        repositories.get()
            .find("repository").stream()
            .filter(repository -> "jcenter".equals(repository.getOptionalElement("id").map(XmlElement::getText).orElse(null)))
            .forEach(XmlElement::remove);
        if (!repositories.get().elements().isEmpty())
            repositories.get().remove();
    }

    private void applyName() {
        if (pom.getOptionalElement("name").isPresent()) return;
        pom.getOrCreateElement("name", furtherUp).setText(pom.getOptionalElement("artifactId").orElseThrow().getText());
    }

    private void applyDescription() {
        if (repository.description == null) return;
        pom.getOrCreateElement("description", furtherUp)
            .setText(repository.description);
    }

    private void applyUrl() {
        if (repository.url == null) return;
        pom.getOrCreateElement("url", furtherUp).setText(repository.url);
    }

    private void applyScm() {
        var scm = pom.getOrCreateElement("scm", furtherDown);
        scm.getOrCreateElement("developerConnection").setText("scm:git:" + origin);
        scm.getOrCreateElement("url").setText(origin);
        scm.getOrCreateElement("tag").setText("HEAD");
    }

    private void applyLicense() {
        if (repository.licenseInfo == null) return;
        var licenses = pom.getOrCreateElement("licenses", furtherDown);
        var license = licenses.getOrCreateElement("license");
        license.getOrCreateElement("name").setText(repository.licenseInfo.name);
        license.getOrCreateElement("url").setText(repository.licenseInfo.url);
        if (!license.hasChildElement("distribution"))
            license.getOrCreateElement("distribution").setText("repo");
    }

    private void applyDistributionManagement() {
        var distributionManagement = pom.getOrCreateElement("distributionManagement", furtherDown);

        var snapshotRepository = distributionManagement.getOrCreateElement("snapshotRepository");
        snapshotRepository.getOrCreateElement("id").setText("ossrh");
        snapshotRepository.getOrCreateElement("url").setText("https://oss.sonatype.org/content/repositories/snapshots");

        var repository = distributionManagement.getOrCreateElement("repository");
        repository.getOrCreateElement("id").setText("ossrh");
        repository.getOrCreateElement("url").setText("https://oss.sonatype.org/service/local/staging/deploy/maven2/");
    }

    private void applyDevelopers() {
        if (repository.collaborators == null || repository.collaborators.totalCount == 0) return;
        var developers = pom.getOrCreateElement("developers", furtherDown);
        for (var collaborator : repository.collaborators.nodes) {
            var found = developers.find("developer/id[text()='" + collaborator.login + "']");
            if (found.isEmpty()) {
                var developer = developers.addElement("developer");
                developer.addElement("id").setText(collaborator.login);
                developer.getOrCreateElement("name").setText(collaborator.name);
            }
        }
    }

    private void applyPlugins() {
        applySourcePlugin();
        applyJavaDocPlugin();
        applyReleasePlugin();
        applyNexusStagingPlugin();
    }

    private void applySourcePlugin() {
        if (hasPlugin("maven-source-plugin")) return;
        var plugin = plugins().addElement("plugin");
        plugin.getOrCreateElement("artifactId").setText("maven-source-plugin");
        plugin.getOrCreateElement("version").setText("3.2.1");
        var executions = plugin.getOrCreateElement("executions");
        var execution = executions.getOrCreateElement("execution");
        execution.getOrCreateElement("id").setText("attach-sources");
        var goals = execution.getOrCreateElement("goals");
        goals.getOrCreateElement("goal").setText("jar-no-fork");
    }

    private XmlElement plugins() {
        return pom
            .getOrCreateElement("build", furtherDown)
            .getOrCreateElement("plugins", atEnd());
    }

    private void applyJavaDocPlugin() {
        if (hasPlugin("maven-javadoc-plugin")) return;
        var plugin = plugins().addElement("plugin");
        plugin.getOrCreateElement("artifactId").setText("maven-javadoc-plugin");
        plugin.getOrCreateElement("version").setText("3.2.0");

        var executions = plugin.getOrCreateElement("executions");
        var execution = executions.getOrCreateElement("execution");
        execution.getOrCreateElement("id").setText("attach-javadocs");
        var goals = execution.getOrCreateElement("goals");
        goals.getOrCreateElement("goal").setText("jar");

        var configuration = plugin.getOrCreateElement("configuration");
        configuration.getOrCreateElement("doclint").setText("-missing");
    }

    private void applyReleasePlugin() {
        if (hasPlugin("maven-release-plugin")) return;
        var plugin = plugins().addElement("plugin");
        plugin.getOrCreateElement("artifactId").setText("maven-release-plugin");
        plugin.getOrCreateElement("version").setText("2.5.3");

        var configuration = plugin.getOrCreateElement("configuration");
        configuration.getOrCreateElement("autoVersionSubmodules").setText("true");
        configuration.getOrCreateElement("tagNameFormat").setText("v@{project.version}");
        configuration.getOrCreateElement("useReleaseProfile").setText("false");
        configuration.getOrCreateElement("releaseProfiles").setText("release");
    }

    private void applyNexusStagingPlugin() {
        if (hasPlugin("nexus-staging-maven-plugin")) return;
        var plugin = plugins().addElement("plugin");
        plugin.getOrCreateElement("groupId").setText("org.sonatype.plugins");
        plugin.getOrCreateElement("artifactId").setText("nexus-staging-maven-plugin");
        plugin.getOrCreateElement("version").setText("1.6.8");

        var configuration = plugin.getOrCreateElement("configuration");
        configuration.getOrCreateElement("serverId").setText("ossrh");
        configuration.getOrCreateElement("nexusUrl").setText("https://oss.sonatype.org/");
        configuration.getOrCreateElement("autoReleaseAfterClose").setText("true");
    }

    private void applyReleaseProfiles() {
        if (hasProfile("release")) return;
        var profile = pom.getOrCreateElement("profiles").addElement("profile");
        profile.getOrCreateElement("id").setText("release");

        var plugins = profile.getOrCreateElement("build").getOrCreateElement("plugins");
        plugins.addComment("always run source+javadoc => see problems early & source available in local repo");

        var plugin = plugins.addElement("plugin");
        plugin.addComment("but don't run gpg everywhere, esp. not in github actions");
        plugin.getOrCreateElement("artifactId").setText("maven-gpg-plugin");
        plugin.getOrCreateElement("version").setText("3.0.1");

        var executions = plugin.getOrCreateElement("executions");
        var execution = executions.getOrCreateElement("execution");
        execution.getOrCreateElement("id").setText("sign-artifacts");
        execution.getOrCreateElement("phase").setText("verify");
        var goals = execution.getOrCreateElement("goals");
        goals.getOrCreateElement("goal").setText("sign");
    }
}
