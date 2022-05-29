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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;

import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;

/**
 * @author Guillaume
 *
 */
public class ErrorAwayRulesDefinition implements RulesDefinition {
	public static final String ERRORPRONE_REPOSITORY = "errorprone";
	public static final String NULLAWAY_REPOSITORY = "nullaway";
	public static final String ERRORPRONE_SLF4J_REPOSITORY = "errorprone-slf4j";
	public static final String AUTODISPOSE2_REPOSITORY = "autodispose2";

	public static final int ERRORPRONE_REPOSITORY_RULES_COUNT = 398;
	public static final int NULLAWAY_REPOSITORY_RULES_COUNT = 1;
	public static final int ERRORPRONE_SLF4J_REPOSITORY_RULES_COUNT = 8;
	public static final int AUTODISPOSE2_REPOSITORY_RULES_COUNT = 1;

	public static final int RULES_COUNT = ERRORPRONE_REPOSITORY_RULES_COUNT 
			+ NULLAWAY_REPOSITORY_RULES_COUNT 
			+ ERRORPRONE_SLF4J_REPOSITORY_RULES_COUNT
			+ AUTODISPOSE2_REPOSITORY_RULES_COUNT;

	protected static final String[] REPOSITORIES = new String[] { 
			ERRORPRONE_REPOSITORY, 
			NULLAWAY_REPOSITORY, 
			ERRORPRONE_SLF4J_REPOSITORY, 
			AUTODISPOSE2_REPOSITORY };

	private static final String[] DESCRIPTION_FOLDERS = new String[] { 
			null, 
			"android", 
			"argumentselectiondefects", 
			"flogger", 
			"inject", 
			"javadoc", 
			"nullness", 
			"time" };

	@Override
	public void define(Context context) {
		NewRepository errorProneRepository = context.createRepository(ERRORPRONE_REPOSITORY, "java").setName("Error Prone");
		NewRepository nullAwayRepository = context.createRepository(NULLAWAY_REPOSITORY, "java").setName("Null Away");
		NewRepository errorProneSlf4jRepository = context.createRepository(ERRORPRONE_SLF4J_REPOSITORY, "java").setName("Error Prone SLF4J");
		NewRepository autodisposeRepository = context.createRepository(AUTODISPOSE2_REPOSITORY, "java").setName("AutoDispose");

		// Built-in checkers
		processCheckers(errorProneRepository, BuiltInCheckerSuppliers.ENABLED_WARNINGS, Severity.MINOR);
		processCheckers(errorProneRepository, BuiltInCheckerSuppliers.ENABLED_ERRORS, Severity.MAJOR);

		// Plugin checkers
		Map<String, NewRepository> pluginRepositories = new HashMap<>();
		pluginRepositories.put(NULLAWAY_REPOSITORY, nullAwayRepository);
		pluginRepositories.put(ERRORPRONE_SLF4J_REPOSITORY, errorProneSlf4jRepository);
		pluginRepositories.put(AUTODISPOSE2_REPOSITORY, autodisposeRepository);

		Map<String, List<Class<? extends BugChecker>>> pluginCheckers = checkerClassesByRepository();

		for (Map.Entry<String, List<Class<? extends BugChecker>>> entry : pluginCheckers.entrySet()) {
			NewRepository repository = pluginRepositories.get(entry.getKey());
			List<Class<? extends BugChecker>> checkers = entry.getValue();

			List<BugCheckerInfo> checkersInfos = checkers.stream().map(BugCheckerInfo::create).collect(Collectors.toList());

			processCheckers(repository, checkersInfos, Severity.MAJOR);
		}

		errorProneRepository.done();
		nullAwayRepository.done();
		errorProneSlf4jRepository.done();
		autodisposeRepository.done();
	}

	public static Map<String, List<Class<? extends BugChecker>>> checkerClassesByRepository() {
		Iterator<BugChecker> checkersIterator = ErrorAwayRulesDefinition.pluginCheckers();

		Map<String, List<Class<? extends BugChecker>>> pluginCheckers = new HashMap<>();
		while (checkersIterator.hasNext()) {
			BugChecker bugChecker = checkersIterator.next();
			String repository = repository(bugChecker.getClass());

			pluginCheckers.computeIfAbsent(repository, r -> new ArrayList<>()).add(bugChecker.getClass());
		}

		return pluginCheckers;
	}

	private void processCheckers(NewRepository repository, Collection<BugCheckerInfo> bugCheckerInfos, String severity) {
		for (BugCheckerInfo bugCheckerInfo : bugCheckerInfos) {
			String ruleKey = asRuleKey(bugCheckerInfo);
			NewRule rule = repository.createRule(ruleKey);

			rule.setName(bugCheckerInfo.canonicalName());
			rule.setSeverity(severity);

			for (String tag : bugCheckerInfo.getTags()) {
				rule.addTags(normalizeTag(tag));
			}

			loadDescription(bugCheckerInfo, ruleKey, rule);
		}
	}

	/**
	 * Rule tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'
	 */
	private String normalizeTag(String tag) {
		return tag.toLowerCase();
	}

	private void loadDescription(BugCheckerInfo bugCheckerInfo, String ruleName, NewRule rule) {
		URL resource = findDescriptionResource(ruleName);
		if (resource != null) {
			rule.setMarkdownDescription(resource);
		} else {
			rule.setMarkdownDescription(bugCheckerInfo.message());
		}
	}

	private URL findDescriptionResource(String ruleName) {
		for (String folder : DESCRIPTION_FOLDERS) {
			URL url = findDescriptionResource(ruleName, folder);

			if (url != null) {
				return url;
			}
		}

		return null;
	}

	private URL findDescriptionResource(String ruleName, String folder) {
		if (folder == null) {
			return getClass().getResource("/errorprone/bugpattern/" + ruleName + ".md");
		} else {
			return getClass().getResource("/errorprone/bugpattern/" + folder + "/" + ruleName + ".md");
		}
	}

	public static Iterator<BugChecker> pluginCheckers() {
		ClassLoader loader = ErrorAwaySensor.class.getClassLoader();

		return ServiceLoader.load(BugChecker.class, loader).iterator();
	}

	public static String asRuleKey(BugCheckerInfo bugCheckerInfo) {
		return bugCheckerInfo.canonicalName();
	}

	public static String asRuleKey(Class<? extends BugChecker> type) {
		return asRuleKey(BugCheckerInfo.create(type));
	}

	public static RuleKey errorProneRuleKey(BugCheckerInfo bugCheckerInfo) {
		return RuleKey.of(ERRORPRONE_REPOSITORY, asRuleKey(bugCheckerInfo));
	}

	public static RuleKey ruleKey(Class<? extends BugChecker> type) {
		return RuleKey.of(repository(type), asRuleKey(type));
	}

	public static String repository(Class<? extends BugChecker> type) {
		String className = type.getName();
		if (className.startsWith("com.uber.nullaway.")) {
			return NULLAWAY_REPOSITORY;
		} else if (className.startsWith("jp.skypencil.errorprone.slf4j.")) {
			return ERRORPRONE_SLF4J_REPOSITORY;
		} else if (className.startsWith("autodispose2.")) {
			return AUTODISPOSE2_REPOSITORY;
		} else {
			throw new ErrorAwayException("Could not find rules repository for class " + className);
		}
	}
}
