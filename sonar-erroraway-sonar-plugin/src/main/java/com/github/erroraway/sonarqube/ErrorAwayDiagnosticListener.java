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
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

import com.github.erroraway.ErrorAwayException;
import com.github.erroraway.rules.ErrorAwayRulesMapping;

/**
 * @author Guillaume
 *
 */
public class ErrorAwayDiagnosticListener implements DiagnosticListener<JavaFileObject> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorAwayDiagnosticListener.class);

	public static final String ERROR_PRONE_COMPILER_CRASH_CODE = "compiler.err.error.prone.crash";
	private static final Set<String> ERROR_PRONE_DIAGNOSTIC_CODES = Set.of("compiler.warn.error.prone",
			"compiler.err.error.prone",
			"compiler.note.error.prone");
	private SensorContext context;

	public ErrorAwayDiagnosticListener(SensorContext context) {
		this.context = context;
	}

	@Override
	public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
		if (!checkDiagnostic(diagnostic)) {
			return;
		}
		String message = diagnostic.getMessage(Locale.ENGLISH);
		String rule = parseRule(diagnostic, message);

		RuleKey ruleKey = RuleKey.of(findRepository(rule, message), rule);

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

	public InputFile getInputFile(Diagnostic<? extends JavaFileObject> diagnostic, FileSystem fs) {
		if (diagnostic.getSource() == null) {
			return null;
		}

		InputFile inputFile = fs.inputFile(fs.predicates().hasURI(diagnostic.getSource().toUri()));

		if (inputFile == null) {
			LOGGER.warn("Could not file input file for source {}", diagnostic.getSource().getName());
		}

		return inputFile;
	}

	/**
	 * @param diagnostic
	 *            The {@link Diagnostic} to check
	 * @return <code>true</code> if the diagnostic is from Error Prone
	 */
	public boolean checkDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
		String code = diagnostic.getCode();

		if (ERROR_PRONE_COMPILER_CRASH_CODE.equals(diagnostic.getCode())) {
			throw new ErrorAwayException("Compiler crash during code analysis, this is most likely a bug in the ErrorAway plugin, not in ErrorProne:\n" + diagnostic);
		}
		if (ERROR_PRONE_DIAGNOSTIC_CODES.contains(code)) {
			return true;
		} else {
			NewAnalysisError analysisError = context.newAnalysisError();
			analysisError.message(diagnostic.getMessage(Locale.ENGLISH));

			FileSystem fs = context.fileSystem();
			InputFile inputFile = getInputFile(diagnostic, fs);

			if (inputFile != null) {
				analysisError.onFile(inputFile);

				try {
					int startLine = Math.max(1, (int) diagnostic.getLineNumber());

					TextPointer location = inputFile.newPointer(startLine, 0);

					analysisError.at(location);
				} catch (IllegalArgumentException e) {
					LOGGER.error("Error setting pointer on file {} for diagnostic {}", inputFile, diagnostic, e);
				}
			}

			analysisError.save();

			if (diagnostic.getKind().equals(Kind.ERROR)) {
				throw new ErrorAwayCompilationException("Compilation error: " + diagnostic);
			}

			return false;
		}
	}

	private String parseRule(Diagnostic<? extends JavaFileObject> diagnostic, String message) {
		try {
			String[] lines = message.split("\n");
			return lines[0].substring(1, lines[0].indexOf(']'));
		} catch (Exception e) {
			throw new ErrorAwayException("Error parsing diagnostic with code: " + diagnostic.getCode() + " and message : " + message);
		}
	}

	private String findRepository(String rule, String message) {
		if (rule.startsWith("Slf4j")) {
			return ErrorAwayRulesMapping.ERRORPRONE_SLF4J_REPOSITORY;
		}
		
		if (message.contains("see https://error-prone.picnic.tech/bugpatterns/")) {
			return ErrorAwayRulesMapping.PICNIC_REPOSITORY;
		}

		switch (rule) {
		case "NullAway":
			return ErrorAwayRulesMapping.NULLAWAY_REPOSITORY;
		default:
			return ErrorAwayRulesMapping.ERRORPRONE_REPOSITORY;
		}
	}
}