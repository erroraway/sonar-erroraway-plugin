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

import java.util.Locale;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * @author Guillaume
 *
 */
public class ErrorAwayDiagnosticListener implements DiagnosticListener<JavaFileObject> {
	private static final Logger LOGGER = Loggers.get(ErrorAwayDiagnosticListener.class);

	private static final Set<String> IGNORED_DIAGNOSTIC_CODES = Set.of("compiler.warn.pkg-info.already.seen",
			"compiler.err.doesnt.exist", 
			"compiler.note.deprecated.filename",
			"compiler.note.deprecated.plural",
			"compiler.note.deprecated.recompile");
	private SensorContext context;

	public ErrorAwayDiagnosticListener(SensorContext context) {
		this.context = context;
	}

	@Override
	public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
		if (ignore(diagnostic)) {
			return;
		}
		String message = diagnostic.getMessage(Locale.ENGLISH);
		String rule = parseRule(diagnostic, message);

		RuleKey ruleKey = RuleKey.of(findRepository(rule), rule);

		int startLine = (int) diagnostic.getLineNumber();

		FileSystem fs = context.fileSystem();
		InputFile inputFile = getInputFile(diagnostic, fs);

		if (inputFile != null) {
			try {
				NewIssue issue = context.newIssue();
				issue.forRule(ruleKey);

				NewIssueLocation location = issue.newLocation();

				location.on(inputFile);
				location.at(inputFile.selectLine(startLine));
				location.message(message);

				issue.at(location);

				issue.save();
			} catch (Exception e) {
				LOGGER.error("Error creating issue for {}", message, e);
			}
		}
	}

	private InputFile getInputFile(Diagnostic<? extends JavaFileObject> diagnostic, FileSystem fs) {
		InputFile inputFile = fs.inputFile(fs.predicates().hasURI(diagnostic.getSource().toUri()));

		if (inputFile == null) {
			LOGGER.warn("Could not file input file for source {}", diagnostic.getSource().getName());
		}

		return inputFile;
	}

	private boolean ignore(Diagnostic<? extends JavaFileObject> diagnostic) {
		return IGNORED_DIAGNOSTIC_CODES.contains(diagnostic.getCode());
	}

	private String parseRule(Diagnostic<? extends JavaFileObject> diagnostic, String message) {
		try {
			String[] lines = message.split("\n");
			return lines[0].substring(1, lines[0].indexOf(']'));
		} catch (Exception e) {
			throw new ErrorAwayException("Error parsing diagnostic with code: " + diagnostic.getCode() + " and message : " + message);
		}
	}

	private String findRepository(String rule) {
		if (rule.startsWith("Slf4j")) {
			return ErrorAwayRulesDefinition.ERRORPRONE_SLF4J_REPOSITORY;
		}

		switch (rule) {
		case "NullAway":
			return ErrorAwayRulesDefinition.NULLAWAY_REPOSITORY;
		case "UseAutoDispose":
			return ErrorAwayRulesDefinition.AUTODISPOSE2_REPOSITORY;
		default:
			return ErrorAwayRulesDefinition.ERRORPRONE_REPOSITORY;
		}
	}
}