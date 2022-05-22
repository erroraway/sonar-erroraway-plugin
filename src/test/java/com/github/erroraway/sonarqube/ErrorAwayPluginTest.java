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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.Plugin.Context;

/**
 * @author Guillaume
 *
 */
class ErrorAwayPluginTest {
	@Test
	void define() {
		Context context = mock(Context.class);

		ErrorAwayPlugin rulesDefinition = new ErrorAwayPlugin();
		rulesDefinition.define(context);

		verify(context, times(3 + NullAwayOption.values().length)).addExtension(Mockito.any());
	}
}
