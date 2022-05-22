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
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;

/**
 * @author Guillaume
 *
 */
class ErrorAwayQualityProfileTest {

	@Test
	void define() {
		Context context = mock(Context.class);
		NewBuiltInQualityProfile builtInQualityProfile = mock(NewBuiltInQualityProfile.class);

		when(context.createBuiltInQualityProfile(Mockito.anyString(), Mockito.anyString())).thenReturn(builtInQualityProfile);

		ErrorAwayQualityProfile qualityProfile = new ErrorAwayQualityProfile();
		qualityProfile.define(context);

		// There's one profile for each plugin plus one profile whith all the plugins so the number of rules is doubled up
		verify(builtInQualityProfile, times(ErrorAwayRulesDefinition.RULES_COUNT * 2)).activateRule(Mockito.anyString(), Mockito.anyString());
	}
}
