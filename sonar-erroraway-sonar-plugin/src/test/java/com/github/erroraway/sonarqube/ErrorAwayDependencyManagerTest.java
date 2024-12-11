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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.TempFolder;

import com.github.erroraway.ErrorAwayException;

/**
 * @author Guillaume Toison
 *
 */
class ErrorAwayDependencyManagerTest {

	@TempDir
	private Path tempDirPath;

	private TempFolder tempFolder;
	private Configuration configuration;

	@BeforeEach
	void setup() {
		configuration = mock(Configuration.class);
		tempFolder = mock(TempFolder.class);

		when(tempFolder.newDir(anyString())).thenAnswer(i -> {
			String name = i.getArgument(0);

			return new File(tempDirPath.toFile(), name);
		});
	}

	@Test
	void invalidSettings() {
		ErrorAwayTestUtil.setConfiguration(configuration, ErrorAwayPluginConstants.MAVEN_USER_SETTINGS_FILE,
				"src/test/resources/samples/invalid-settings.xml");

		assertThrows(ErrorAwayException.class, () -> new ErrorAwayDependencyManager(tempFolder, configuration));
	}

	@Test
	void invalidArtifactCoordinates() {
		when(configuration.getBoolean(ErrorAwayPluginConstants.MAVEN_WORK_OFFLINE)).thenReturn(Optional.of(true));
		ErrorAwayDependencyManager dependencyManager = new ErrorAwayDependencyManager(tempFolder, configuration);

		assertThrows(ErrorAwayException.class, () -> dependencyManager.downloadDependencies("x:y:1.2.3"));
	}
}
