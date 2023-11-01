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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import com.github.erroraway.rules.ErrorAwayRulesMapping;

/**
 * @author Guillaume
 *
 */
public class ErrorAwayQualityProfile implements BuiltInQualityProfilesDefinition {

	public static final String ERROR_PRONE_PROFILE_NAME = "Error Prone";
	public static final String NULL_AWAY_PROFILE_NAME = "Null Away";
	public static final String ERROR_PRONE_SLF4J_PROFILE_NAME = "Error Prone SLF4J";
	public static final String AUTODISPOSE2_PROFILE_NAME = "Autodispose2";
	public static final String PICNIC_PROFILE_NAME = "Picnic Error Prone Support";
	public static final String ERROR_PRONE_AND_PLUGINS_PROFILE_NAME = "Error Prone and plugins";
	
	private RuleFinder ruleFinder;
	
	public ErrorAwayQualityProfile(RuleFinder ruleFinder) {
		this.ruleFinder = ruleFinder;
	}

	@Override
	public void define(Context context) {
		NewBuiltInQualityProfile errorProneProfile = context.createBuiltInQualityProfile(ERROR_PRONE_PROFILE_NAME, "java");
		NewBuiltInQualityProfile nullAwayProfile = context.createBuiltInQualityProfile(NULL_AWAY_PROFILE_NAME, "java");
		NewBuiltInQualityProfile errorProneSlf4jProfile = context.createBuiltInQualityProfile(ERROR_PRONE_SLF4J_PROFILE_NAME, "java");
		NewBuiltInQualityProfile autodisposeProfile = context.createBuiltInQualityProfile(AUTODISPOSE2_PROFILE_NAME, "java");
		NewBuiltInQualityProfile picnicEerrorProneProfile = context.createBuiltInQualityProfile(PICNIC_PROFILE_NAME, "java");
		NewBuiltInQualityProfile errorProneAndPluginsProfile = context.createBuiltInQualityProfile(ERROR_PRONE_AND_PLUGINS_PROFILE_NAME, "java");

		Map<String, NewBuiltInQualityProfile> pluginRepositories = new HashMap<>();		
		pluginRepositories.put(ErrorAwayRulesMapping.ERRORPRONE_REPOSITORY, errorProneProfile);
		pluginRepositories.put(ErrorAwayRulesMapping.NULLAWAY_REPOSITORY, nullAwayProfile);
		pluginRepositories.put(ErrorAwayRulesMapping.ERRORPRONE_SLF4J_REPOSITORY, errorProneSlf4jProfile);
		pluginRepositories.put(ErrorAwayRulesMapping.AUTODISPOSE2_REPOSITORY, autodisposeProfile);
		pluginRepositories.put(ErrorAwayRulesMapping.PICNIC_REPOSITORY, picnicEerrorProneProfile);

		for (String repoKey : ErrorAwayRulesMapping.REPOSITORIES) {
			RuleQuery query = RuleQuery.create().withRepositoryKey(repoKey);
			Collection<Rule> rules = ruleFinder.findAll(query);
			NewBuiltInQualityProfile repository = pluginRepositories.get(repoKey);

			processCheckers(repository, rules, repoKey);
			processCheckers(errorProneAndPluginsProfile, rules, repoKey);
		}

		errorProneProfile.done();
		nullAwayProfile.done();
		errorProneSlf4jProfile.done();
		autodisposeProfile.done();
		picnicEerrorProneProfile.done();
		errorProneAndPluginsProfile.done();
	}

	private void processCheckers(NewBuiltInQualityProfile profile, Collection<Rule> rules, String repoKey) {
		for (Rule rule : rules) {
			profile.activateRule(repoKey, rule.getKey());
		}
	}
}
