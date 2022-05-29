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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
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
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;

/**
 * @author Guillaume
 *
 */
public class ErrorAwayIT {

	private static final String SIMPLE_PROJECT_KEY = "sonar-error-away-plugin:simple";

	private static Orchestrator ORCHESTRATOR;

	private static QualityprofilesService QUALITY_PROFILES_SERVICE;
	private static ProjectsService PROJECT_SERVICES;
	private static IssuesService ISSUES_SERVICES;

	@BeforeAll
	public static void startOrchestrator() {
		String sonarVersion = System.getProperty("sonar.version", "9.4");
	    
	    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
				.addPlugin(FileLocation.of("./target/sonar-erroraway-plugin.jar"))
	    		.keepBundledPlugins()
				.setSonarVersion("LATEST_RELEASE[" + sonarVersion + "]");

		ORCHESTRATOR = orchestratorBuilder.build();
		ORCHESTRATOR.start();

		String baseUrl = ORCHESTRATOR.getServer().getUrl();
		HttpConnector connector = HttpConnector.newBuilder().url(baseUrl).credentials("admin", "admin").build();

		QUALITY_PROFILES_SERVICE = new QualityprofilesService(connector);
		PROJECT_SERVICES = new ProjectsService(connector);
		ISSUES_SERVICES = new IssuesService(connector);
	}
	
	@AfterAll
	public static void stopOrchestrator() {
		ORCHESTRATOR.stop();
	}

	@Test
	void analyzeSimpleProject() {
		// Create the sample project
		CreateRequest createRequest = new CreateRequest();
		createRequest.setName("ErrorAway");
		createRequest.setProject(SIMPLE_PROJECT_KEY);
		PROJECT_SERVICES.create(createRequest);

		// Enable the quality profile
		AddProjectRequest addProjectRequest = new AddProjectRequest();
		addProjectRequest.setLanguage("java");
		addProjectRequest.setProject(SIMPLE_PROJECT_KEY);
		addProjectRequest.setQualityProfile(ErrorAwayQualityProfile.ERROR_PRONE_AND_PLUGINS_PROFILE_NAME);
		QUALITY_PROFILES_SERVICE.addProject(addProjectRequest);

		MavenBuild build = MavenBuild.create()
				.setPom(new File("src/test/resources/projects/simple/pom.xml").getAbsoluteFile())
				.setProperty(NullAwayOption.ANNOTATED_PACKAGES.getKey(), "com.bugs,application")
				.setProperty("sonar.host.url", ORCHESTRATOR.getServer().getUrl())
				.setProperty("sonar.login", "admin")
				.setProperty("sonar.password", "admin")
				.setGoals("clean package org.sonarsource.scanner.maven:sonar-maven-plugin:sonar");
		
		ORCHESTRATOR.executeBuild(build);

		// Check the issues reported in SonarQube
		SearchRequest issueRequest = new SearchRequest();
		issueRequest.setProjects(Collections.singletonList(SIMPLE_PROJECT_KEY));
		List<Issue> issues = ISSUES_SERVICES.search(issueRequest).getIssuesList();

		assertThat(issues.size(), is(22));

		assertSimpleIssues(issues);
		assertAndroidActivityIssues(issues);
		assertApplicationSimpleIssues(issues);
		assertBugsSamplesIssues(issues);
		assertHibernateEntityIssues(issues);
		assertAutoValueSamplesIssues(issues);
        assertGrammarListenerIssues(issues);
	}

	@SuppressWarnings("unchecked")
	private void assertSimpleIssues(List<Issue> issues) {
		Predicate<Issue> simpleJavaPredicate = component(SIMPLE_PROJECT_KEY, "src/main/java/Simple.java");
		assertThat(issues, containsIssueMatching(simpleJavaPredicate, rule("errorprone:DefaultPackage"), startLine(1)));
		assertThat(issues, containsIssueMatching(simpleJavaPredicate, rule("errorprone-slf4j:Slf4jLoggerShouldBePrivate"), startLine(8)));
		assertThat(issues, containsIssueMatching(simpleJavaPredicate, rule("errorprone:ClassNewInstance"), startLine(11)));
		assertThat(issues, containsIssueMatching(simpleJavaPredicate, rule("errorprone:EqualsNaN"), startLine(20)));
		assertThat(issues, containsIssueMatching(simpleJavaPredicate, rule("errorprone:UnusedMethod"), startLine(25)));
		assertThat(issues, containsIssueMatching(simpleJavaPredicate, rule("errorprone-slf4j:Slf4jPlaceholderMismatch"), startLine(26)));
	}

	@SuppressWarnings("unchecked")
	private void assertAndroidActivityIssues(List<Issue> issues) {
		Predicate<Issue> androidActivityJavaPredicate = component(SIMPLE_PROJECT_KEY, "src/main/java/application/AndroidActivity.java");
		assertThat(issues, containsIssueMatching(androidActivityJavaPredicate, rule("errorprone:CheckReturnValue"), startLine(15)));
	}

	@SuppressWarnings("unchecked")
	private void assertApplicationSimpleIssues(List<Issue> issues) {
		Predicate<Issue> applicationSimpleJavaPredicate = component(SIMPLE_PROJECT_KEY, "src/main/java/application/Simple.java");
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("errorprone:ClassNewInstance"), startLine(15)));
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("nullaway:NullAway"), startLine(22)));
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("errorprone:EqualsNaN"), startLine(24)));
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("errorprone:CollectionIncompatibleType"), startLine(33)));
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("errorprone:UnusedMethod"), startLine(38)));
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("nullaway:NullAway"), startLine(45)));
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("errorprone:UnusedMethod"), startLine(48)));
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("errorprone-slf4j:Slf4jPlaceholderMismatch"), startLine(49)));
	}

	@SuppressWarnings("unchecked")
	private void assertBugsSamplesIssues(List<Issue> issues) {
		Predicate<Issue> bugsJavaPredicate = component(SIMPLE_PROJECT_KEY, "src/main/java/com/bugs/BugsSamples.java");
		assertThat(issues, containsIssueMatching(bugsJavaPredicate, rule("errorprone:ZoneIdOfZ"), startLine(8)));
	}

	@SuppressWarnings("unchecked")
	private void assertHibernateEntityIssues(List<Issue> issues) {
		Predicate<Issue> applicationSimpleJavaPredicate = component(SIMPLE_PROJECT_KEY, "src/main/java/application/HibernateEntity.java");
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("nullaway:NullAway"), startLine(15)));
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("nullaway:NullAway"), startLine(16)));
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("errorprone:DurationTemporalUnit"), startLine(31)));
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("nullaway:NullAway"), startLine(32)));
	}

	@SuppressWarnings("unchecked")
	private void assertGrammarListenerIssues(List<Issue> issues) {
		Predicate<Issue> applicationSimpleJavaPredicate = component(SIMPLE_PROJECT_KEY,	"src/main/java/application/GrammarListener.java");
		assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("errorprone:MissingOverride"), startLine(8)));
	}

    @SuppressWarnings("unchecked")
    private void assertAutoValueSamplesIssues(List<Issue> issues) {
        Predicate<Issue> applicationSimpleJavaPredicate = component(SIMPLE_PROJECT_KEY, "src/main/java/application/AutoValueSamples.java");
        assertThat(issues, containsIssueMatching(applicationSimpleJavaPredicate, rule("errorprone:DurationTemporalUnit"), startLine(13)));
    }

	@SuppressWarnings("unchecked")
	private Matcher<List<Issue>> containsIssueMatching(Predicate<Issue>... issuePredicates) {
		Predicate<Issue> issuePredicate = i -> Stream.of(issuePredicates).allMatch(p -> p.test(i));
		
		return new BaseMatcher<List<Issue>>() {

			@Override
			public boolean matches(Object item) {
				if (item instanceof List) {
					List<Issue> issues = (List<Issue>) item;

					return issues.stream().anyMatch(issuePredicate);
				}
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Issues list must contain a matching item");
			}
		};
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
