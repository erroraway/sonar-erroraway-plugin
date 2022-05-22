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
package com.github.erroraway.sonarqube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.plugins.java.api.JavaResourceLocator;

/**
 * @author Guillaume
 *
 */
class ErrorAwaySensorTest {

	@RegisterExtension
	LogTesterJUnit5 logTester = new LogTesterJUnit5();

	private Configuration configuration;
	private FileSystem fs;
	private SensorContext context;
	private ActiveRules activeRules;
	private FilePredicates filePredicates;
	private JavaResourceLocator javaResourceLocator;
	private NewIssue newIssue;
	private NewIssueLocation location;

	private FilePredicate mainJavaFilePredicate;
	private FilePredicate uriFilePredicate;
	private InputFile inputFile;

	@BeforeEach
	void setup() {
		Path path = Path.of("src/test/resources/samples");
		Path relativePath = Path.of("com/bug/BugSamples.java");
		Charset charset = Charset.forName("UTF-8");
		
		// Mocked dependencies
		configuration = mock(Configuration.class);
		fs = mock(FileSystem.class);
		context = mock(SensorContext.class);
		activeRules = mock(ActiveRules.class);
		filePredicates = mock(FilePredicates.class);
		javaResourceLocator = mock(JavaResourceLocator.class);
		newIssue = mock(NewIssue.class);
		location = mock(NewIssueLocation.class);

		// Sample data
		FilePredicate javaFilePredicate = inputFile -> inputFile.filename().endsWith("java");
		FilePredicate mainFilePredicate = inputFile -> inputFile.type() == Type.MAIN;
		mainJavaFilePredicate = inputFile -> javaFilePredicate.apply(inputFile) && mainFilePredicate.apply(inputFile);
		uriFilePredicate = inputFile -> inputFile.uri().equals(inputFile.uri());
		inputFile = new TestInputFile(path.resolve(relativePath), relativePath, charset, Type.MAIN);
		Iterable<InputFile> inputFiles = Collections.singleton(inputFile);
		Collection<File> classpath = Collections.singleton(new File(System.getProperty("java.home") + "/lib/jrt-fs.jar"));

		// Mock some methods
		when(context.config()).thenReturn(configuration);
		when(context.activeRules()).thenReturn(activeRules);
		when(context.fileSystem()).thenReturn(fs);

		when(fs.encoding()).thenReturn(charset);
		when(fs.predicates()).thenReturn(filePredicates);
		when(fs.inputFiles(mainJavaFilePredicate)).thenReturn(inputFiles);
		when(fs.inputFile(uriFilePredicate)).thenReturn(inputFile);
		
		when(filePredicates.hasLanguage("java")).thenReturn(javaFilePredicate);
		when(filePredicates.hasType(Type.MAIN)).thenReturn(mainFilePredicate);
		when(filePredicates.and(mainFilePredicate, javaFilePredicate)).thenReturn(mainJavaFilePredicate);
		when(filePredicates.hasURI(inputFile.uri())).thenReturn(uriFilePredicate);
		when(javaResourceLocator.classpath()).thenReturn(classpath);
		
		when(context.newIssue()).thenReturn(newIssue);

		when(newIssue.newLocation()).thenReturn(location);
	}

	@Test
	void analyzeWithErrorProneRule() {
		when(activeRules.find(RuleKey.of("errorprone", "DurationTemporalUnit"))).thenReturn(mock(ActiveRule.class));

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator);
		sensor.execute(context);

		verify(context, times(1)).newIssue();
	}

	@Test
	void analyzeWithNullAway() {
		when(activeRules.find(RuleKey.of("nullaway", "NullAway"))).thenReturn(mock(ActiveRule.class));
		when(configuration.hasKey(NullAwayOption.ANNOTATED_PACKAGES.getKey())).thenReturn(true);
		when(configuration.getStringArray(NullAwayOption.ANNOTATED_PACKAGES.getKey())).thenReturn(new String[] { "foo", "com.bug", "bar" });

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator);
		sensor.execute(context);

		verify(context, times(1)).newIssue();
	}

	@Test
	void analyzeWithNullAwayWithoutAnnotatedPackageOption() {
		when(activeRules.find(RuleKey.of("nullaway", "NullAway"))).thenReturn(mock(ActiveRule.class));

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator);
		assertThrows(ErrorAwayException.class, () -> sensor.execute(context));
	}

	@Test
	void missingInputFile() {
		when(activeRules.find(RuleKey.of("errorprone", "DurationTemporalUnit"))).thenReturn(mock(ActiveRule.class));
		when(fs.inputFile(uriFilePredicate)).thenReturn(null);

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator);
		sensor.execute(context);

		assertThat(logTester.getLogs(LoggerLevel.WARN).stream().map(LogAndArguments::getRawMsg).collect(Collectors.toList())).contains("Could not file input file for source {}");
	}

	@Test
	void describe() {
		SensorDescriptor descriptor = mock(SensorDescriptor.class);

		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator);
		sensor.describe(descriptor);

		verify(descriptor, times(1)).onlyOnLanguage("java");
	}
}
