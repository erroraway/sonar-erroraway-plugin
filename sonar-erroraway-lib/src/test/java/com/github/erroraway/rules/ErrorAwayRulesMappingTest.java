package com.github.erroraway.rules;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.erroraway.ErrorAwayException;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.bugpatterns.BugChecker;

class ErrorAwayRulesMappingTest {

	@Test
	void unknownRepository() {
		assertThrows(ErrorAwayException.class, () -> ErrorAwayRulesMapping.repository(UnknownBugChecker.class));
	}

	@BugPattern(summary = "", severity = SeverityLevel.ERROR)
	private static class UnknownBugChecker extends BugChecker {
		private static final long serialVersionUID = 1L;

	}
}
