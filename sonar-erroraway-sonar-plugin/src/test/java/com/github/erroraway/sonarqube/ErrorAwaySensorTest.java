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
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.TempFolder;
import org.sonar.plugins.java.api.JavaResourceLocator;

import com.github.erroraway.ErrorAwayException;
import com.github.erroraway.rules.ErrorAwayRulesMapping;

/**
 * @author Guillaume Toison
 *
 */
class ErrorAwaySensorTest {

	@RegisterExtension
	private LogTesterJUnit5 logTester = new LogTesterJUnit5();

	@TempDir
	private Path tempDirPath;

	private Configuration configuration;
	private FileSystem fs;
	private SensorContext context;
	private ActiveRules activeRules;
	private FilePredicates filePredicates;
	private JavaResourceLocator javaResourceLocator;
	private TempFolder tempFolder;
	private NewIssue newIssue;
	private NewIssueLocation location;
	private NewAnalysisError analisysError;
	private ErrorAwayDependencyManager dependencyManager;

	private FilePredicate mainJavaFilePredicate;
	private FilePredicate uriFilePredicate;
	private InputFile inputFile;

	@BeforeEach
	void setup() {
		configuration = mock(Configuration.class);
		tempFolder = mock(TempFolder.class);

		when(tempFolder.newDir(anyString())).thenAnswer(i -> {
			String name = i.getArgument(0);

			File dir = new File(tempDirPath.toFile(), name);
			dir.mkdir();

			return dir;
		});
	}

	/**
	 * @param relativePath The path of the source file (e.g. com/bug/BugSamples.java)
	 */
	void setup(Path relativePath) {
		Path path = Path.of("src/test/resources/samples");
		Charset charset = Charset.forName("UTF-8");
		
		// Mocked dependencies
		fs = mock(FileSystem.class);
		context = mock(SensorContext.class);
		activeRules = mock(ActiveRules.class);
		filePredicates = mock(FilePredicates.class);
		javaResourceLocator = mock(JavaResourceLocator.class);
		newIssue = mock(NewIssue.class);
		location = mock(NewIssueLocation.class);
		analisysError = mock(NewAnalysisError.class);

		dependencyManager = new ErrorAwayDependencyManager(tempFolder, configuration);

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
		when(context.newAnalysisError()).thenReturn(analisysError);

		when(newIssue.newLocation()).thenReturn(location);
	}

	private void setConfigurationStringArray(String key, String[] value) {
		ErrorAwayTestUtil.setConfigurationStringArray(configuration, key, value);
	}

	private void setConfigurationBoolean(String key, boolean value) {
		ErrorAwayTestUtil.setConfigurationBoolean(configuration, key, value);
	}

	private void enableRule(RuleKey ruleKey) {
		when(activeRules.find(ruleKey)).thenReturn(mock(ActiveRule.class));
	}

	@Test
	void analyzeWithErrorProneRule() {
		setup(Path.of("com/bug/BugSamples.java"));
		enableRule(RuleKey.of("errorprone", "DurationTemporalUnit"));

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
		sensor.execute(context);

		verify(context, times(1)).newIssue();
	}

	@Test
	void analyzeWithAnnotationProcessor() {
		setConfigurationStringArray(ErrorAwayPlugin.CLASS_PATH_MAVEN_COORDINATES, new String[]{"com.google.auto.value:auto-value-annotations:1.9"});
		setConfigurationStringArray(ErrorAwayPlugin.ANNOTATION_PROCESSORS_MAVEN_COORDINATES, new String[]{"com.google.auto.value:auto-value:1.9"});
		setConfigurationBoolean(ErrorAwayPlugin.MAVEN_USE_TEMP_LOCAL_REPOSITORY, true);
		setConfigurationStringArray(ErrorAwayPlugin.MAVEN_REPOSITORIES, new String[]{"https://repo1.maven.org/maven2/"});
	
		setup(Path.of("com/bug/AutoValueSamples.java"));
		enableRule(RuleKey.of("errorprone", "DurationTemporalUnit"));

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
		sensor.execute(context);

		verify(context, times(1)).newIssue();
	}

	@Test
	void missingDependencies() {
		setup(Path.of("com/bug/AutoValueSamples.java"));
		enableRule(RuleKey.of("errorprone", "DurationTemporalUnit"));

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
		// Javac wraps our exception in a RuntimeException
		assertThrowsExactly(RuntimeException.class, () -> sensor.execute(context));

		if (JRE.currentVersion().compareTo(JRE.JAVA_21) < 0) {
			verify(context, times(1)).newAnalysisError();
		} else {
			// Since JDK 21 one warning about implicit annotation processing
			verify(context, times(2)).newAnalysisError();
		}
	}

	@Test
	void analyzeWithNullAway() {
		setup(Path.of("com/bug/BugSamples.java"));
		when(activeRules.find(RuleKey.of("nullaway", "NullAway"))).thenReturn(mock(ActiveRule.class));
		
		setConfigurationStringArray(NullAwayOption.ANNOTATED_PACKAGES.getKey(), new String[] { "foo", "com.bug", "bar" });

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
		sensor.execute(context);

		verify(context, times(1)).newIssue();
	}

	@Test
	void analyzeWithNullAwayWithoutAnnotatedPackageOption() {
		setup(Path.of("com/bug/BugSamples.java"));
		when(activeRules.find(RuleKey.of("nullaway", "NullAway"))).thenReturn(mock(ActiveRule.class));

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
		assertThrows(ErrorAwayException.class, () -> sensor.execute(context));
	}
	
    @Test
    void analyzeWithErrorProneSlf4j() {
        setConfigurationStringArray(ErrorAwayPlugin.MAVEN_REPOSITORIES, new String[] {"https://repo1.maven.org/maven2/"});
        setup(Path.of("com/bug/Slf4jSamples.java"));
        
		RuleKey ruleKey = RuleKey.of("errorprone-slf4j", "Slf4jPlaceholderMismatch");
		enableRule(ruleKey);
        setConfigurationStringArray(ErrorAwayPlugin.CLASS_PATH_MAVEN_COORDINATES, new String[]{"org.slf4j:slf4j-api:1.7.36"});
        
        // Call the sensor
        ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
        sensor.execute(context);

        verify(context, times(1)).newIssue();
		verify(newIssue, times(1)).forRule(ruleKey);
    }

	@Test
	void analyzeWithPicnicErrorProneSupport() {
		setConfigurationStringArray(ErrorAwayPlugin.MAVEN_REPOSITORIES, new String[]{"https://repo1.maven.org/maven2/"});
		setup(Path.of("com/bug/PicnicErrorProneSupportSample.java"));

		RuleKey ruleKey = RuleKey.of(ErrorAwayRulesMapping.PICNIC_REPOSITORY, "IdentityConversion");
		enableRule(ruleKey);
		setConfigurationStringArray(
				ErrorAwayPlugin.CLASS_PATH_MAVEN_COORDINATES,
				new String[]{"com.google.guava:guava:31.1-jre"});

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
		sensor.execute(context);

		verify(context, times(1)).newIssue();
		verify(newIssue, times(1)).forRule(ruleKey);
	}

    @Test
    void analyzeManyBugs() {
        setup(Path.of("com/bug/ManyBugs.java"));
        
        enableRule(RuleKey.of("errorprone", "BadShiftAmount"));
        enableRule(RuleKey.of("errorprone", "ComparingThisWithNull"));
        enableRule(RuleKey.of("errorprone", "EqualsNaN"));
        enableRule(RuleKey.of("errorprone", "NullTernary"));
        
        // Call the sensor
        ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
        sensor.execute(context);

        verify(context, times(256)).newIssue();
    }

	@Test
	void compilerWarning() {
		setup(Path.of("com/bug/VarArgsArray.java"));
		enableRule(RuleKey.of("errorprone", "DurationTemporalUnit"));

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
		sensor.execute(context);

		if (JRE.currentVersion().compareTo(JRE.JAVA_21) < 0) {
			// One compiler warning that there are unsage operations in the file and another to recompile for more details
			verify(context, times(2)).newAnalysisError();
		} else {
			// Since JDK 21 one warning about implicit annotation processing
			verify(context, times(3)).newAnalysisError();
		}
	}

	@Test
	void missingInputFile() {
		setup(Path.of("com/bug/BugSamples.java"));
		enableRule(RuleKey.of("errorprone", "DurationTemporalUnit"));
		when(fs.inputFile(uriFilePredicate)).thenReturn(null);

		// Call the sensor
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
		sensor.execute(context);

		assertThat(logTester.getLogs(Level.WARN).stream().map(LogAndArguments::getRawMsg).collect(Collectors.toList())).contains("Could not file input file for source {}");
	}

	@Test
	void describe() {
		setup(Path.of("com/bug/BugSamples.java"));
		SensorDescriptor descriptor = mock(SensorDescriptor.class);

		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);
		sensor.describe(descriptor);

		verify(descriptor, times(1)).onlyOnLanguage("java");
	}
	
	@Test
	void getVersion() {
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);

		assertThat(sensor.getVersion()).doesNotStartWith("UNKNOWN");
	}
	
	@Test
	void getVersionError() {
		ErrorAwaySensor sensor = new ErrorAwaySensor(javaResourceLocator, dependencyManager, tempFolder);

		assertThat(sensor.getVersion("/foo/bar.properties")).startsWith("UNKNOWN");
	}
}
