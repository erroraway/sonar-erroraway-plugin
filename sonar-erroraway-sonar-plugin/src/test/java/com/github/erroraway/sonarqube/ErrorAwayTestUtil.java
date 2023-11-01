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

import static org.mockito.Mockito.when;

import java.util.Optional;

import org.sonar.api.config.Configuration;

/**
 * @author Guillaume Toison
 *
 */
public class ErrorAwayTestUtil {

	public static void setConfigurationStringArray(Configuration configuration, String key, String[] value) {
		when(configuration.hasKey(key)).thenReturn(true);
		when(configuration.getStringArray(key)).thenReturn(value);
	}

	public static void setConfiguration(Configuration configuration, String key, String value) {
		when(configuration.hasKey(key)).thenReturn(true);
		when(configuration.get(key)).thenReturn(Optional.of(value));
	}

	public static void setConfigurationBoolean(Configuration configuration, String key, boolean value) {
		when(configuration.hasKey(key)).thenReturn(true);
		when(configuration.getBoolean(key)).thenReturn(Optional.of(value));
	}
}
