package com.github.erroraway.maven;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ErrorAwayRulesMojoTest {

	@Test
	void execute() {
		ErrorAwayRulesMojo mojo = new ErrorAwayRulesMojo();
		
		assertDoesNotThrow(mojo::execute);
	}
}
