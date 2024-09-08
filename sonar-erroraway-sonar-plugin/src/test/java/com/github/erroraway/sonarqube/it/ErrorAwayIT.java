/*
 * Copyright 2022 The ErrorAway Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.erroraway.sonarqube.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.issues.IssuesService;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.projects.CreateRequest;
import org.sonarqube.ws.client.projects.ProjectsService;
import org.sonarqube.ws.client.qualityprofiles.AddProjectRequest;
import org.sonarqube.ws.client.qualityprofiles.QualityprofilesService;

import com.github.erroraway.sonarqube.ErrorAwayQualityProfile;
import com.github.erroraway.sonarqube.NullAwayOption;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.GradleBuild;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.junit5.OrchestratorExtensionBuilder;
import com.sonar.orchestrator.locator.FileLocation;

/**
 * @author Guillaume
 *
 */
@IntegrationTest
public class ErrorAwayIT {

	private static final String SIMPLE_MAVEN_PROJECT_KEY = "sonar-error-away-plugin:simple";
	private static final String SIMPLE_GRADLE_PROJECT_KEY = "sonar-error-away-plugin:gradle-simple";

	private static Orchestrator ORCHESTRATOR;

	private static QualityprofilesService QUALITY_PROFILES_SERVICE;
	private static ProjectsService PROJECT_SERVICES;
	private static IssuesService ISSUES_SERVICES;

	@BeforeAll
	public static void startOrchestrator() {
		String sonarVersion = System.getProperty("sonar.server.version", "9.5");
		System.out.println(FileLocation.of("./target/sonar-erroraway-plugin.jar"));
		OrchestratorExtensionBuilder orchestratorBuilder = OrchestratorExtension.builderEnv()
				// Since SQ 9.8 permissions for 'Anyone' group has been limited for new instances
				.useDefaultAdminCredentialsForBuilds(true)
				.addPlugin(FileLocation.of("./target/sonar-erroraway-plugin.jar"))
				.keepBundledPlugins()
				.setOrchestratorProperty("orchestrator.artifactory.url", "https://repo1.maven.org/maven2")
				.setServerProperty("sonar.web.port", getSonarWebPort())
				.setSonarVersion("LATEST_RELEASE[" + sonarVersion + "]");

		ORCHESTRATOR = orchestratorBuilder.build();
		ORCHESTRATOR.start();

		String baseUrl = ORCHESTRATOR.getServer().getUrl();
		HttpConnector connector = HttpConnector.newBuilder().url(baseUrl).credentials("admin", "admin").build();

		QUALITY_PROFILES_SERVICE = new QualityprofilesService(connector);
		PROJECT_SERVICES = new ProjectsService(connector);
		ISSUES_SERVICES = new IssuesService(connector);
	}

	private static String getSonarWebPort() {
		return System.getProperty("sonar.web.port", "9000");
	}

	@AfterAll
	public static void stopOrchestrator() {
		ORCHESTRATOR.stop();
	}

	private void setupProjectAndProfile(String projectKey, String projectName) {
		// Create the sample project
		CreateRequest createRequest = new CreateRequest();
		createRequest.setName(projectName);
		createRequest.setProject(projectKey);
		PROJECT_SERVICES.create(createRequest);

		// Enable the quality profile
		AddProjectRequest addProjectRequest = new AddProjectRequest();
		addProjectRequest.setLanguage("java");
		addProjectRequest.setProject(projectKey);
		addProjectRequest.setQualityProfile(ErrorAwayQualityProfile.ERROR_PRONE_AND_PLUGINS_PROFILE_NAME);
		QUALITY_PROFILES_SERVICE.addProject(addProjectRequest);
	}

	@Test
	void analyzeSimpleMavenProject() {
		setupProjectAndProfile(SIMPLE_MAVEN_PROJECT_KEY, "Simple - Maven");

		MavenBuild build = MavenBuild.create()
				.setPom(new File("src/test/resources/projects/simple/pom.xml").getAbsoluteFile())
				.setProperty(NullAwayOption.ANNOTATED_PACKAGES.getKey(), "com.bugs,application")
				.setProperty("sonar.host.url", ORCHESTRATOR.getServer().getUrl())
				.setProperty("sonar.login", "admin")
				.setProperty("sonar.password", "admin")
				.setProperty("sonar.web.port", getSonarWebPort())
				.setGoals("clean package org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.plugins.downloadOnlyRequired=false");

		ORCHESTRATOR.executeBuild(build);

		checkIssues(SIMPLE_MAVEN_PROJECT_KEY);
	}

	@Test
	void analyzeSimpleGradleProject() {
		setupProjectAndProfile(SIMPLE_GRADLE_PROJECT_KEY, "Simple - Gradle");

		// Can't seem to set property nullaway.annotated.packages here, so it is set in build.gradle
		GradleBuild build = GradleBuild.create()
				.setProjectDirectory(FileLocation.of(new File("src/test/resources/projects/simple").getAbsoluteFile()))
				.setProperty("sonar.host.url", ORCHESTRATOR.getServer().getUrl())
				.setProperty("sonar.login", "admin")
				.setProperty("sonar.password", "admin")
				.setProperty("sonar.web.port", getSonarWebPort())
				.setTasks("clean", "build")
				.addArgument("--stacktrace")
				.addArgument("-Dsonar.plugins.downloadOnlyRequired=false")
				.addSonarTask();

		ORCHESTRATOR.executeBuild(build);

		checkIssues(SIMPLE_GRADLE_PROJECT_KEY);
	}

	private void checkIssues(String projectKey) {
		// Check the issues reported in SonarQube
		SearchRequest issueRequest = new SearchRequest();
		issueRequest.setProjects(Collections.singletonList(projectKey));
		List<Issue> issues = ISSUES_SERVICES.search(issueRequest).getIssuesList();

		assertThat(issues).hasSize(22);

		assertSimpleIssues(issues, projectKey);
		assertApplicationSimpleIssues(issues, projectKey);
		assertBugsSamplesIssues(issues, projectKey);
		assertHibernateEntityIssues(issues, projectKey);
		assertPicnicSamplesIssues(issues, projectKey);
		assertGrammarListenerIssues(issues, projectKey);
	}

	@SuppressWarnings("unchecked")
	private void assertSimpleIssues(List<Issue> issues, String projectKey) {
		Predicate<Issue> simpleJavaPredicate = component(projectKey, "src/main/java/Simple.java");

		assertThatIssuesContainsIssueMatching(issues, simpleJavaPredicate, rule("errorprone:DefaultPackage"), startLine(1));
		assertThatIssuesContainsIssueMatching(issues, simpleJavaPredicate, rule("errorprone-slf4j:Slf4jLoggerShouldBePrivate"), startLine(8));
		assertThatIssuesContainsIssueMatching(issues, simpleJavaPredicate, rule("errorprone:ClassNewInstance"), startLine(11));
		assertThatIssuesContainsIssueMatching(issues, simpleJavaPredicate, rule("errorprone:ComparisonOutOfRange"), startLine(20));
		assertThatIssuesContainsIssueMatching(issues, simpleJavaPredicate, rule("errorprone:UnusedMethod"), startLine(25));
		assertThatIssuesContainsIssueMatching(issues, simpleJavaPredicate, rule("errorprone-slf4j:Slf4jPlaceholderMismatch"), startLine(26));
	}

	@SuppressWarnings("unchecked")
	private void assertApplicationSimpleIssues(List<Issue> issues, String projectKey) {
		Predicate<Issue> applicationSimpleJavaPredicate = component(projectKey, "src/main/java/application/Simple.java");
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("errorprone:ClassNewInstance"), startLine(15));
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("nullaway:NullAway"), startLine(22));
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("errorprone:ComparisonOutOfRange"), startLine(24));
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("errorprone:CollectionIncompatibleType"), startLine(33));
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("errorprone:UnusedMethod"), startLine(38));
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("nullaway:NullAway"), startLine(45));
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("errorprone:UnusedMethod"), startLine(48));
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("errorprone-slf4j:Slf4jPlaceholderMismatch"), startLine(49));
	}

	@SuppressWarnings("unchecked")
	private void assertBugsSamplesIssues(List<Issue> issues, String projectKey) {
		Predicate<Issue> bugsJavaPredicate = component(projectKey, "src/main/java/com/bugs/BugsSamples.java");
		assertThatIssuesContainsIssueMatching(issues, bugsJavaPredicate, rule("errorprone:ZoneIdOfZ"), startLine(8));
	}

	@SuppressWarnings("unchecked")
	private void assertHibernateEntityIssues(List<Issue> issues, String projectKey) {
		Predicate<Issue> applicationSimpleJavaPredicate = component(projectKey, "src/main/java/application/HibernateEntity.java");
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("nullaway:NullAway"), startLine(15));
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("nullaway:NullAway"), startLine(16));
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("errorprone:DurationTemporalUnit"), startLine(31));
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("nullaway:NullAway"), startLine(32));
	}

	@SuppressWarnings("unchecked")
	private void assertPicnicSamplesIssues(List<Issue> issues, String projectKey) {
		Predicate<Issue> applicationSimpleJavaPredicate = component(projectKey,	"src/main/java/application/GrammarListener.java");
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("picnic-errorprone:EmptyMethod"), startLine(12));
	}

	@SuppressWarnings("unchecked")
	private void assertGrammarListenerIssues(List<Issue> issues, String projectKey) {
		Predicate<Issue> applicationSimpleJavaPredicate = component(projectKey,	"src/main/java/application/GrammarListener.java");
		assertThatIssuesContainsIssueMatching(issues, applicationSimpleJavaPredicate, rule("errorprone:MissingOverride"), startLine(8));
	}

	@SuppressWarnings("unchecked")
	private void assertThatIssuesContainsIssueMatching(List<Issue> issues, Predicate<Issue>... issuePredicates) {
		Predicate<Issue> issuePredicate = i -> Stream.of(issuePredicates).allMatch(p -> p.test(i));

		assertThat(issues).anyMatch(issuePredicate);
	}

	private Predicate<Issue> component(String projectKey, String fileName) {
		return i -> i.getComponent().equals(projectKey + ":" + fileName);
	}

	private Predicate<Issue> rule(String ruleKey) {
		return i -> i.getRule().equals(ruleKey);
	}

	private Predicate<Issue> startLine(int line) {
		return i -> i.getTextRange().getStartLine() == line;
	}
}
