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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;

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

	@Override
	public void define(Context context) {
		NewBuiltInQualityProfile errorProneProfile = context.createBuiltInQualityProfile(ERROR_PRONE_PROFILE_NAME, "java");
		NewBuiltInQualityProfile nullAwayProfile = context.createBuiltInQualityProfile(NULL_AWAY_PROFILE_NAME, "java");
		NewBuiltInQualityProfile errorProneSlf4jProfile = context.createBuiltInQualityProfile(ERROR_PRONE_SLF4J_PROFILE_NAME, "java");
		NewBuiltInQualityProfile autodisposeProfile = context.createBuiltInQualityProfile(AUTODISPOSE2_PROFILE_NAME, "java");
		NewBuiltInQualityProfile picnicEerrorProneProfile = context.createBuiltInQualityProfile(PICNIC_PROFILE_NAME, "java");
        NewBuiltInQualityProfile errorProneAndPluginsProfile = context.createBuiltInQualityProfile(ERROR_PRONE_AND_PLUGINS_PROFILE_NAME, "java");

		// Built-in checkers
		processCheckers(errorProneProfile, BuiltInCheckerSuppliers.ENABLED_WARNINGS, ErrorAwayRulesDefinition.ERRORPRONE_REPOSITORY);
		processCheckers(errorProneProfile, BuiltInCheckerSuppliers.ENABLED_ERRORS, ErrorAwayRulesDefinition.ERRORPRONE_REPOSITORY);

		processCheckers(errorProneAndPluginsProfile, BuiltInCheckerSuppliers.ENABLED_WARNINGS, ErrorAwayRulesDefinition.ERRORPRONE_REPOSITORY);
		processCheckers(errorProneAndPluginsProfile, BuiltInCheckerSuppliers.ENABLED_ERRORS, ErrorAwayRulesDefinition.ERRORPRONE_REPOSITORY);

		// Plugin checkers
		Map<String, NewBuiltInQualityProfile> pluginRepositories = new HashMap<>();
		pluginRepositories.put(ErrorAwayRulesDefinition.NULLAWAY_REPOSITORY, nullAwayProfile);
		pluginRepositories.put(ErrorAwayRulesDefinition.ERRORPRONE_SLF4J_REPOSITORY, errorProneSlf4jProfile);
		pluginRepositories.put(ErrorAwayRulesDefinition.AUTODISPOSE2_REPOSITORY, autodisposeProfile);
        pluginRepositories.put(ErrorAwayRulesDefinition.PICNIC_REPOSITORY, picnicEerrorProneProfile);

		Map<String, List<Class<? extends BugChecker>>> pluginCheckers = ErrorAwayRulesDefinition.checkerClassesByRepository();

		for (Map.Entry<String, List<Class<? extends BugChecker>>> entry : pluginCheckers.entrySet()) {
			String repoKey = entry.getKey();
			NewBuiltInQualityProfile repository = pluginRepositories.get(repoKey);
			List<Class<? extends BugChecker>> checkers = entry.getValue();

			List<BugCheckerInfo> checkersInfos = checkers.stream().map(BugCheckerInfo::create).collect(Collectors.toList());

			processCheckers(repository, checkersInfos, repoKey);
			processCheckers(errorProneAndPluginsProfile, checkersInfos, repoKey);
		}

		errorProneProfile.done();
		nullAwayProfile.done();
		errorProneSlf4jProfile.done();
		autodisposeProfile.done();
		picnicEerrorProneProfile.done();
		errorProneAndPluginsProfile.done();
	}

	private void processCheckers(NewBuiltInQualityProfile profile, Collection<BugCheckerInfo> bugCheckerInfos, String repoKey) {
		for (BugCheckerInfo bugCheckerInfo : bugCheckerInfos) {
			profile.activateRule(repoKey, ErrorAwayRulesDefinition.asRuleKey(bugCheckerInfo));
		}
	}
}
