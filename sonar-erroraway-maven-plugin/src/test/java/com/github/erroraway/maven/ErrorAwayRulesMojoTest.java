package com.github.erroraway.maven;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

class ErrorAwayRulesMojoTest {

	@Test
	void execute() {
		ErrorAwayRulesMojo mojo = new ErrorAwayRulesMojo();
		mojo.outputDirectory = new File("target/test/metadata");
		
		assertDoesNotThrow(mojo::execute);
	}
}
