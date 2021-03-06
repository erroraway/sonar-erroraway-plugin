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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.NewRepository;
import org.sonar.api.server.rule.RulesDefinition.NewRule;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.bugpatterns.BugChecker;

/**
 * @author Guillaume
 *
 */
class ErrorAwayRulesDefinitionTest {

	@Test
	void define() {
		Context context = mock(Context.class);
		NewRepository newRepository = mock(NewRepository.class);
		NewRule newRule = mock(NewRule.class);

		when(context.createRepository(Mockito.anyString(), Mockito.anyString())).thenReturn(newRepository);

		when(newRepository.createRule(Mockito.anyString())).thenReturn(newRule);
		when(newRepository.setName(Mockito.anyString())).thenReturn(newRepository);

		ErrorAwayRulesDefinition rulesDefinition = new TestErrorAwayRulesDefinition();
		rulesDefinition.define(context);
		
		verify(context, times(1)).createRepository(ErrorAwayRulesDefinition.ERRORPRONE_REPOSITORY, "java");
		verify(context, times(1)).createRepository(ErrorAwayRulesDefinition.NULLAWAY_REPOSITORY, "java");
		verify(context, times(1)).createRepository(ErrorAwayRulesDefinition.ERRORPRONE_SLF4J_REPOSITORY, "java");
		verify(context, times(1)).createRepository(ErrorAwayRulesDefinition.AUTODISPOSE2_REPOSITORY, "java");

		verify(newRepository, times(ErrorAwayRulesDefinition.RULES_COUNT)).createRule(Mockito.anyString());
	}

	@Test
	void unknownRepository() {
		assertThrows(ErrorAwayException.class, () -> ErrorAwayRulesDefinition.repository(UnknownBugChecker.class));
	}

	@BugPattern(summary = "", severity = SeverityLevel.ERROR)
	private static class UnknownBugChecker extends BugChecker {
		private static final long serialVersionUID = 1L;

	}
	
	/**
	 * Injects errors to test the exception handling
	 */
	private static class TestErrorAwayRulesDefinition extends ErrorAwayRulesDefinition {
	    private boolean hasThrownExceptionForInputStreamReader;
        private boolean hasThrownExceptionForStringReader;
	    
	    @Override
	    public String convertMdToHtml(Reader reader) throws IOException {
	        if (!hasThrownExceptionForInputStreamReader && reader instanceof InputStreamReader) {
                hasThrownExceptionForInputStreamReader = true;
                throw new IOException("Test exception");
            } else if (!hasThrownExceptionForStringReader && reader instanceof StringReader) {
                hasThrownExceptionForStringReader = true;
                throw new IOException("Test exception");
            } else {
	            return super.convertMdToHtml(reader);
	        }
	    }
	}
}
