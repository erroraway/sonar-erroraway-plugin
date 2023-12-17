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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.github.erroraway.sonarqube.ErrorAwayQualityProfile.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;

import com.github.erroraway.rules.ErrorAwayRulesMapping;

/**
 * @author Guillaume
 *
 */
class ErrorAwayQualityProfileTest {

	@Test
	void define() {
		Context context = mock(Context.class);
		RuleFinder ruleFinder = mock(RuleFinder.class);
		
		NewBuiltInQualityProfile errorProneProfile = mock(NewBuiltInQualityProfile.class);
		NewBuiltInQualityProfile nullAwayProfile = mock(NewBuiltInQualityProfile.class);
		NewBuiltInQualityProfile errorProneSlf4jProfile = mock(NewBuiltInQualityProfile.class);
		NewBuiltInQualityProfile picnicEerrorProneProfile = mock(NewBuiltInQualityProfile.class);
		NewBuiltInQualityProfile errorProneAndPluginsProfile = mock(NewBuiltInQualityProfile.class);

		when(context.createBuiltInQualityProfile(ERROR_PRONE_PROFILE_NAME, "java")).thenReturn(errorProneProfile);
		when(context.createBuiltInQualityProfile(NULL_AWAY_PROFILE_NAME, "java")).thenReturn(nullAwayProfile);
		when(context.createBuiltInQualityProfile(ERROR_PRONE_SLF4J_PROFILE_NAME, "java")).thenReturn(errorProneSlf4jProfile);
		when(context.createBuiltInQualityProfile(PICNIC_PROFILE_NAME, "java")).thenReturn(picnicEerrorProneProfile);
		when(context.createBuiltInQualityProfile(ERROR_PRONE_AND_PLUGINS_PROFILE_NAME, "java")).thenReturn(errorProneAndPluginsProfile);
		
		when(ruleFinder.findAll(any(RuleQuery.class))).then(i -> {
			RuleQuery query = i.getArgument(0, RuleQuery.class);
			String repository = query.getRepositoryKey();
			
			ErrorAwayRulesDefinition rulesDefinition = new ErrorAwayRulesDefinition();
			String resourceFolder = rulesDefinition.resourceFolder(repository);
			List<String> rulesKeys = rulesDefinition.loadRepositoryRuleKeys(resourceFolder);
			List<Rule> rules = new ArrayList<>();
			
			for (String ruleKey : rulesKeys) {
				Rule rule = mock(Rule.class);
				when(rule.getKey()).thenReturn(ruleKey);
				
				rules.add(rule);
			}
			
			return rules;
		});

		ErrorAwayQualityProfile qualityProfile = new ErrorAwayQualityProfile(ruleFinder);
		qualityProfile.define(context);

		verify(errorProneProfile, times(ErrorAwayRulesMapping.ERRORPRONE_REPOSITORY_RULES_COUNT)).activateRule(Mockito.anyString(), Mockito.anyString());
		verify(nullAwayProfile, times(ErrorAwayRulesMapping.NULLAWAY_REPOSITORY_RULES_COUNT)).activateRule(Mockito.anyString(), Mockito.anyString());
		verify(errorProneSlf4jProfile, times(ErrorAwayRulesMapping.ERRORPRONE_SLF4J_REPOSITORY_RULES_COUNT)).activateRule(Mockito.anyString(), Mockito.anyString());
		verify(picnicEerrorProneProfile, times(ErrorAwayRulesMapping.PICNIC_REPOSITORY_RULES_COUNT)).activateRule(Mockito.anyString(), Mockito.anyString());
		verify(errorProneAndPluginsProfile, times(ErrorAwayRulesMapping.RULES_COUNT)).activateRule(Mockito.anyString(), Mockito.anyString());
	}
}
