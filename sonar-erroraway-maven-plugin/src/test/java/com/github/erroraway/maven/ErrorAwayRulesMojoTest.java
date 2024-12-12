package com.github.erroraway.maven;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;

class ErrorAwayRulesMojoTest {

	@Test
	void execute() {
		ErrorAwayRulesMojo mojo = new ErrorAwayRulesMojo();
		mojo.outputDirectory = new File("target/test/metadata");

		assertDoesNotThrow(mojo::execute);
	}

	@Test
	void handleDescriptionReadException() throws Exception {
		ErrorAwayRulesMojo mojo = spy(ErrorAwayRulesMojo.class);
		mojo.outputDirectory = new File("target/test/metadata");

		when(mojo.findDescriptionResource(anyString())).thenReturn(new URL("file://doesnotexist"));

		assertThatCode(mojo::execute).hasMessageStartingWith("Error parsing MD description");

		verify(mojo, times(1)).handleDescriptionReadException(anyString(), any());
	}
}
