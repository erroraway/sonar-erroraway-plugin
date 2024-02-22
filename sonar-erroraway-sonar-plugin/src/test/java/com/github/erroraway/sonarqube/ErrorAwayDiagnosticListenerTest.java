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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.NewAnalysisError;

import com.github.erroraway.ErrorAwayException;

/**
 * @author Guillaume Toison
 *
 */
class ErrorAwayDiagnosticListenerTest {
	private SensorContext context;
	private NewAnalysisError analysisError;
	private FileSystem fs;
	private FilePredicates filePredicates;

	@BeforeEach
	void setup() {
		context = mock(SensorContext.class);
		analysisError = mock(NewAnalysisError.class);
		fs = mock(FileSystem.class);
		filePredicates = mock(FilePredicates.class);

		when(context.newAnalysisError()).thenReturn(analysisError);
		when(context.fileSystem()).thenReturn(fs);

		when(fs.predicates()).thenReturn(filePredicates);
	}

	@Test
	@SuppressWarnings("unchecked")
	void failOnCompilerError() {
		Diagnostic<JavaFileObject> diagnostic = mock(Diagnostic.class);
		JavaFileObject javaFileObject = mock(JavaFileObject.class);

		when(diagnostic.getCode()).thenReturn("compiler.note.deprecated.filename");
		when(diagnostic.getSource()).thenReturn(javaFileObject);
		when(diagnostic.getKind()).thenReturn(Kind.ERROR);

		ErrorAwayDiagnosticListener listener = new ErrorAwayDiagnosticListener(context);

		assertThrows(ErrorAwayCompilationException.class, () -> listener.report(diagnostic));

		verify(context, never()).newIssue();
	}

	@Test
	@SuppressWarnings("unchecked")
	void compilerCrash() {
		Diagnostic<JavaFileObject> diagnostic = mock(Diagnostic.class);
		when(diagnostic.getCode()).thenReturn(ErrorAwayDiagnosticListener.ERROR_PRONE_COMPILER_CRASH_CODE);

		ErrorAwayDiagnosticListener listener = new ErrorAwayDiagnosticListener(context);

		assertThrows(ErrorAwayException.class, () -> listener.checkDiagnostic(diagnostic));
	}

	@Test
	@SuppressWarnings("unchecked")
	void invalidErrorDiagnostic() {
		Diagnostic<JavaFileObject> diagnostic = mock(Diagnostic.class);
		when(diagnostic.getCode()).thenReturn("compiler.warn.error.prone");
		when(diagnostic.getMessage(any())).thenReturn("[Xyz 13213");

		ErrorAwayDiagnosticListener listener = new ErrorAwayDiagnosticListener(context);

		assertThrows(ErrorAwayException.class, () -> listener.report(diagnostic));
	}

	@Test
	@SuppressWarnings("unchecked")
	void nullSourceDiagnostic() {
		Diagnostic<JavaFileObject> diagnostic = mock(Diagnostic.class);
		when(diagnostic.getSource()).thenReturn(null);

		ErrorAwayDiagnosticListener listener = new ErrorAwayDiagnosticListener(context);

		assertThat(listener.getInputFile(diagnostic, fs)).isNull();
	}
}