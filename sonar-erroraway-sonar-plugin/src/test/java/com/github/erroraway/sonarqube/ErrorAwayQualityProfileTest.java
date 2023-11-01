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
		
		NewBuiltInQualityProfile builtInQualityProfile = mock(NewBuiltInQualityProfile.class);

		when(context.createBuiltInQualityProfile(Mockito.anyString(), Mockito.anyString())).thenReturn(builtInQualityProfile);
		
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

		// There's one profile for each plugin plus one profile whith all the plugins so the number of rules is doubled up
		verify(builtInQualityProfile, times(ErrorAwayRulesMapping.RULES_COUNT * 2)).activateRule(Mockito.anyString(), Mockito.anyString());
	}
}
