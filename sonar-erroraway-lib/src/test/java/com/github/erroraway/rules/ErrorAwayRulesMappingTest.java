package com.github.erroraway.rules;

import static com.github.erroraway.rules.ErrorAwayRulesMapping.ERRORPRONE_REPOSITORY;
import static com.github.erroraway.rules.ErrorAwayRulesMapping.ERRORPRONE_SLF4J_REPOSITORY;
import static com.github.erroraway.rules.ErrorAwayRulesMapping.NULLAWAY_REPOSITORY;
import static com.github.erroraway.rules.ErrorAwayRulesMapping.PICNIC_REPOSITORY;
import static com.github.erroraway.rules.ErrorAwayRulesMapping.repository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.erroraway.ErrorAwayException;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.JavaUtilDateChecker;
import com.uber.nullaway.NullAway;

import jp.skypencil.errorprone.slf4j.Slf4jLoggerShouldBePrivate;
import tech.picnic.errorprone.bugpatterns.JUnitValueSource;

class ErrorAwayRulesMappingTest {

	@Test
	void mapping() {
		assertThat(repository(JavaUtilDateChecker.class)).isEqualTo(ERRORPRONE_REPOSITORY);
		assertThat(repository(NullAway.class)).isEqualTo(NULLAWAY_REPOSITORY);
		assertThat(repository(Slf4jLoggerShouldBePrivate.class)).isEqualTo(ERRORPRONE_SLF4J_REPOSITORY);
		assertThat(repository(JUnitValueSource.class)).isEqualTo(PICNIC_REPOSITORY);
	}
	
	@Test
	void unknownRepository() {
		assertThrows(ErrorAwayException.class, () -> ErrorAwayRulesMapping.repository(UnknownBugChecker.class));
	}

	@BugPattern(summary = "", severity = SeverityLevel.ERROR)
	private static class UnknownBugChecker extends BugChecker {
		private static final long serialVersionUID = 1L;

	}
}
