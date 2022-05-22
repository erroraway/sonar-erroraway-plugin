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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.SensorContext;

/**
 * @author Guillaume
 *
 */
class ErrorAwayDiagnosticListenerTest {
	private SensorContext context;

	@BeforeEach
	void setup() {
		context = mock(SensorContext.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void ignoreCompilerError() {
		Diagnostic<? extends JavaFileObject> diagnostic = mock(Diagnostic.class);
		when(diagnostic.getCode()).thenReturn("compiler.note.deprecated.filename");

		ErrorAwayDiagnosticListener listener = new ErrorAwayDiagnosticListener(context);

		listener.report(diagnostic);

		verify(context, never()).newIssue();
	}
}