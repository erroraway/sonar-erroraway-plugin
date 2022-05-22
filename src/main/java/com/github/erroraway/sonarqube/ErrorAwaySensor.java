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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.java.api.JavaResourceLocator;

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
	private JavaResourceLocator javaResourceLocator;

	public ErrorAwaySensor(JavaResourceLocator javaResourceLocator) {
		this.javaResourceLocator = javaResourceLocator;
	}

	@Override
	public void execute(SensorContext context) {
		List<Class<? extends BugChecker>> checkers = new ArrayList<>();

		// Built-in checkers
		addErrorProneCheckers(context, checkers, BuiltInCheckerSuppliers.ENABLED_WARNINGS);
		addErrorProneCheckers(context, checkers, BuiltInCheckerSuppliers.ENABLED_ERRORS);

		// Plugin checkers
		Iterator<BugChecker> checkersIterator = ErrorAwayRulesDefinition.pluginCheckers();

		while (checkersIterator.hasNext()) {
			BugChecker bugChecker = checkersIterator.next();

			if (isCheckerActive(context, bugChecker)) {
				checkers.add(bugChecker.getClass());
			}
		}

		// ErrorProne options
		ErrorProneOptions errorProneOptions = buildErrorProneOptions(context);

		// Setup the compiler and analyze the code
		ScannerSupplier scannerSupplier = ScannerSupplier.fromBugCheckerClasses(checkers).applyOverrides(errorProneOptions);
		JavaCompiler compiler = new ErrorProneJavaCompiler(scannerSupplier);

		DiagnosticListener<? super JavaFileObject> diagnosticListener = new ErrorAwayDiagnosticListener(context);

		Iterable<String> classes = Collections.emptyList();

		FileSystem fs = context.fileSystem();

		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticListener, Locale.getDefault(), fs.encoding())) {
			Iterable<? extends JavaFileObject> compilationUnits = buildCompilationUnits(context, fileManager);

			CompilationTask task = compiler.getTask(null, fileManager, diagnosticListener, Collections.emptyList(), classes, compilationUnits);
			task.call();
		} catch (IOException e) {
			throw new ErrorAwayException("Error analyzing project", e);
		}
	}

	private ErrorProneOptions buildErrorProneOptions(SensorContext context) {
		Configuration configuration = context.config();
		List<String> options = new ArrayList<>();

		// Fail here if the option was not set instead a showing a giant stacktrace
		if (!configuration.hasKey(NullAwayOption.ANNOTATED_PACKAGES.getKey()) 
				&& context.activeRules().find(RuleKey.of("nullaway", "NullAway")) != null) {
			throw new ErrorAwayException("The " + NullAwayOption.ANNOTATED_PACKAGES.getKey() + " option must be set when the NullAway rule is enabled");
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

	private boolean isCheckerActive(SensorContext context, BugChecker bugChecker) {
		RuleKey ruleKey = ErrorAwayRulesDefinition.ruleKey(bugChecker.getClass());

		return context.activeRules().find(ruleKey) != null;
	}

	private Iterable<? extends JavaFileObject> buildCompilationUnits(SensorContext context, StandardJavaFileManager fileManager) throws IOException {
		FileSystem fs = context.fileSystem();
		FilePredicates p = fs.predicates();

		List<JavaFileObject> paths = new ArrayList<>();
		// javaResourceLocator.classpath() does not give test dependencies so we only analyze the main files
		for (InputFile inputFile : fs.inputFiles(p.and(p.hasType(Type.MAIN), p.hasLanguage("java")))) {
			paths.add(new InputFileJavaFileObject(inputFile));
		}

		fileManager.setLocation(StandardLocation.CLASS_PATH, javaResourceLocator.classpath());

		return paths;
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
		descriptor.createIssuesForRuleRepository(ErrorAwayRulesDefinition.REPOSITORIES);
		descriptor.onlyOnLanguage("java");
		descriptor.name("Errorprone sensor");
	}
}
