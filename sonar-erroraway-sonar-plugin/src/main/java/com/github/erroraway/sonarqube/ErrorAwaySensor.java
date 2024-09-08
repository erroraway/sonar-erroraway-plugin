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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.TempFolder;
import org.sonar.java.classpath.ClasspathForMain;

import com.github.erroraway.ErrorAwayException;
import com.github.erroraway.rules.ErrorAwayRulesMapping;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.ErrorProneJavaCompiler;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
import com.google.errorprone.scanner.ScannerSupplier;

/**
 * @author Guillaume Toison
 *
 */
public class ErrorAwaySensor implements Sensor {
	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorAwaySensor.class);

	private ErrorAwayDependencyManager dependencyManager;
	private TempFolder tempFolder;

	public ErrorAwaySensor(ErrorAwayDependencyManager dependencyManager, TempFolder tempFolder) {
		this.dependencyManager = dependencyManager;
		this.tempFolder = tempFolder;
	}

	@Override
	public void execute(SensorContext context) {
		List<Class<? extends BugChecker>> checkers = new ArrayList<>();

		// Built-in checkers
		addErrorProneCheckers(context, checkers, BuiltInCheckerSuppliers.ENABLED_WARNINGS);
		addErrorProneCheckers(context, checkers, BuiltInCheckerSuppliers.ENABLED_ERRORS);

		// Plugin checkers
		Iterator<BugChecker> checkersIterator = ErrorAwayRulesMapping.pluginCheckers();

		while (checkersIterator.hasNext()) {
			BugChecker bugChecker = checkersIterator.next();

			if (isCheckerActive(context, bugChecker)) {
				checkers.add(bugChecker.getClass());
			}
		}

		// Compiler options
		ErrorProneOptions errorProneOptions = buildErrorProneOptions(context);
		List<String> javacOptions = buildJavacOptions();

		// Setup the compiler and analyze the code
		ScannerSupplier scannerSupplier = ScannerSupplier.fromBugCheckerClasses(checkers).applyOverrides(errorProneOptions);
		JavaCompiler compiler = new ErrorProneJavaCompiler(scannerSupplier);

		DiagnosticListener<? super JavaFileObject> diagnosticListener = new ErrorAwayDiagnosticListener(context);

		Iterable<String> classes = Collections.emptyList();

		FileSystem fs = context.fileSystem();

		LOGGER.info("Starting project analysis with encoding {} and base dir {}, plugin version is: {}, java version is {}", fs.encoding(), fs.baseDir(), getVersion(), Runtime.version());

		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticListener, Locale.getDefault(), fs.encoding())) {
			Iterable<? extends JavaFileObject> compilationUnits = buildCompilationUnits(context);

			configureClasspath(fileManager, context.config(), fs);
			configureAnnotationProcessors(fileManager, context.config());
			configureOutputFolders(fileManager);

			CompilationTask task = compiler.getTask(null, fileManager, diagnosticListener, javacOptions, classes, compilationUnits);
			task.call();
		} catch (IOException e) {
			throw new ErrorAwayException("Error analyzing project", e);
		}
	}

	private ErrorProneOptions buildErrorProneOptions(SensorContext context) {
		Configuration configuration = context.config();
		List<String> options = new ArrayList<>();

		// Fail here if the option was not set instead a showing a giant stacktrace
		if (!configuration.hasKey(NullAwayOption.ANNOTATED_PACKAGES.getKey())) {
			if (context.activeRules().find(RuleKey.of("nullaway", "NullAway")) != null) {
				throw new ErrorAwayException("The " + NullAwayOption.ANNOTATED_PACKAGES.getKey() + " option must be set when the NullAway rule is enabled");
			}

			// When some annotation processors are enabled com.google.errorprone.ErrorPronePlugins turns on plugin
			// scanning and tries to instanciate NullAway
			if (configuration.hasKey(ErrorAwayPlugin.ANNOTATION_PROCESSORS_MAVEN_COORDINATES)) {
				options.add("-XepOpt:NullAway:" + NullAwayOption.ANNOTATED_PACKAGES.getErrorproneOption() + "=foo.bar");
			}
		}

		for (NullAwayOption option : NullAwayOption.values()) {
			String key = option.getKey();

			if (configuration.hasKey(key)) {
				String[] values = configuration.getStringArray(key);

				options.add("-XepOpt:NullAway:" + option.getErrorproneOption() + "=" + String.join(",", values));
			}
		}

		return ErrorProneOptions.processArgs(options);
	}

	private List<String> buildJavacOptions() {
		List<String> options = new ArrayList<>();

		// By default javac gives up after 100 errors
		// Surely in the future we'll have programs with more bugs, but we are limited
		// by the technology of our time
		options.add("-Xmaxerrs");
		options.add(Integer.toString(Integer.MAX_VALUE));

		options.add("-Xmaxwarns");
		options.add(Integer.toString(Integer.MAX_VALUE));

		return options;
	}

	private boolean isCheckerActive(SensorContext context, BugChecker bugChecker) {
		RuleKey ruleKey = ErrorAwayRulesDefinition.ruleKey(bugChecker.getClass());

		return context.activeRules().find(ruleKey) != null;
	}

	private Iterable<? extends JavaFileObject> buildCompilationUnits(SensorContext context) {
		FileSystem fs = context.fileSystem();
		FilePredicates p = fs.predicates();

		List<JavaFileObject> paths = new ArrayList<>();
		// javaResourceLocator.classpath() does not give test dependencies so we only analyze the main files
		for (InputFile inputFile : fs.inputFiles(p.and(p.hasType(Type.MAIN), p.hasLanguage("java")))) {
			paths.add(new InputFileJavaFileObject(inputFile));
		}

		return paths;
	}

	private void configureClasspath(StandardJavaFileManager fileManager, Configuration configuration, FileSystem fs) throws IOException {
		ClasspathForMain classpathForMain = new ClasspathForMain(configuration, fs);
		Collection<File> classpath = new ArrayList<>(classpathForMain.getElements());
		if (configuration.hasKey(ErrorAwayPlugin.CLASS_PATH_MAVEN_COORDINATES)) {
			String[] coordinates = configuration.getStringArray(ErrorAwayPlugin.CLASS_PATH_MAVEN_COORDINATES);
			classpath.addAll(dependencyManager.downloadDependencies(coordinates));
		}

		fileManager.setLocation(StandardLocation.CLASS_PATH, classpath);
	}

	private void configureAnnotationProcessors(StandardJavaFileManager fileManager, Configuration configuration) throws IOException {
		if (configuration.hasKey(ErrorAwayPlugin.ANNOTATION_PROCESSORS_MAVEN_COORDINATES)) {
			String[] coordinates = configuration.getStringArray(ErrorAwayPlugin.ANNOTATION_PROCESSORS_MAVEN_COORDINATES);
			Collection<File> annotationProcessors = dependencyManager.downloadDependencies(coordinates);

			fileManager.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, annotationProcessors);
		}
	}

	private void configureOutputFolders(StandardJavaFileManager fileManager) throws IOException {
		fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, Collections.singletonList(tempFolder.newDir("sourceOutput")));
		fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(tempFolder.newDir("classOutput")));
	}

	private void addErrorProneCheckers(SensorContext context, List<Class<? extends BugChecker>> checkers, Set<BugCheckerInfo> checkersInfos) {
		for (BugCheckerInfo bugCheckerInfo : checkersInfos) {
			if (context.activeRules().find(ErrorAwayRulesDefinition.errorProneRuleKey(bugCheckerInfo)) != null) {
				checkers.add(bugCheckerInfo.checkerClass());
			}
		}
	}

	@Override
	public void describe(SensorDescriptor descriptor) {
		for (String repository : ErrorAwayRulesMapping.REPOSITORIES) {
			descriptor.createIssuesForRuleRepository(repository);
		}

		descriptor.onlyOnLanguage("java");
		descriptor.name("Errorprone sensor");
	}

	protected String getVersion() {
		return getVersion("/com/github/erroraway/sonarqube/erroraway-plugin.properties");
	}

	protected String getVersion(String propertiesPath) {
		try (InputStream input = getClass().getResourceAsStream(propertiesPath)) {
			Properties properties = new Properties();
			properties.load(input);

			return properties.getProperty("erroraway.plugin.version");
		} catch (Exception e) {
			LOGGER.debug("Could not find version", e);
			return "UNKNOWN: " + e.getMessage();
		}
	}
}
