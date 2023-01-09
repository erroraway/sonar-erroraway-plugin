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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;

/**
 * @author Guillaume Toison
 *
 */
public class ErrorAwayRulesDefinition implements RulesDefinition {
	private static final Logger LOGGER = Loggers.get(ErrorAwayRulesDefinition.class);
	
	public static final String ERRORPRONE_REPOSITORY = "errorprone";
	public static final String NULLAWAY_REPOSITORY = "nullaway";
	public static final String ERRORPRONE_SLF4J_REPOSITORY = "errorprone-slf4j";
	public static final String AUTODISPOSE2_REPOSITORY = "autodispose2";
	public static final String PICNIC_REPOSITORY = "picnic-errorprone";

	public static final int ERRORPRONE_REPOSITORY_RULES_COUNT = 411;
	public static final int NULLAWAY_REPOSITORY_RULES_COUNT = 1;
	public static final int ERRORPRONE_SLF4J_REPOSITORY_RULES_COUNT = 8;
	public static final int AUTODISPOSE2_REPOSITORY_RULES_COUNT = 1;
	public static final int PICNIC_REPOSITORY_RULES_COUNT = 32;

	public static final int RULES_COUNT = ERRORPRONE_REPOSITORY_RULES_COUNT 
			+ NULLAWAY_REPOSITORY_RULES_COUNT 
			+ ERRORPRONE_SLF4J_REPOSITORY_RULES_COUNT
			+ AUTODISPOSE2_REPOSITORY_RULES_COUNT
			+ PICNIC_REPOSITORY_RULES_COUNT;

	protected static final String[] REPOSITORIES = new String[] { 
			ERRORPRONE_REPOSITORY, 
			NULLAWAY_REPOSITORY, 
			ERRORPRONE_SLF4J_REPOSITORY, 
			AUTODISPOSE2_REPOSITORY,
			PICNIC_REPOSITORY};

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
		NewRepository picnicErrorProneSupportRepository = context.createRepository(PICNIC_REPOSITORY, "java").setName("Picnic Error Prone Support");

		// Built-in checkers
		processCheckers(errorProneRepository, BuiltInCheckerSuppliers.ENABLED_WARNINGS);
		processCheckers(errorProneRepository, BuiltInCheckerSuppliers.ENABLED_ERRORS);

		// Plugin checkers
		Map<String, NewRepository> pluginRepositories = new HashMap<>();
		pluginRepositories.put(NULLAWAY_REPOSITORY, nullAwayRepository);
		pluginRepositories.put(ERRORPRONE_SLF4J_REPOSITORY, errorProneSlf4jRepository);
		pluginRepositories.put(AUTODISPOSE2_REPOSITORY, autodisposeRepository);
		pluginRepositories.put(PICNIC_REPOSITORY, picnicErrorProneSupportRepository);

		Map<String, List<Class<? extends BugChecker>>> pluginCheckers = checkerClassesByRepository();

		for (Map.Entry<String, List<Class<? extends BugChecker>>> entry : pluginCheckers.entrySet()) {
			NewRepository repository = pluginRepositories.get(entry.getKey());
			List<Class<? extends BugChecker>> checkers = entry.getValue();

			List<BugCheckerInfo> checkersInfos = checkers.stream().map(BugCheckerInfo::create).collect(Collectors.toList());

			processCheckers(repository, checkersInfos);
		}

		errorProneRepository.done();
		nullAwayRepository.done();
		errorProneSlf4jRepository.done();
		autodisposeRepository.done();
		picnicErrorProneSupportRepository.done();
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

	private void processCheckers(NewRepository repository, Collection<BugCheckerInfo> bugCheckerInfos) {
		for (BugCheckerInfo bugCheckerInfo : bugCheckerInfos) {
			String ruleKey = asRuleKey(bugCheckerInfo);
			NewRule rule = repository.createRule(ruleKey);

			rule.setName(bugCheckerInfo.canonicalName());
			rule.setSeverity(getSeverity(bugCheckerInfo));

			for (String tag : bugCheckerInfo.getTags()) {
				rule.addTags(normalizeTag(tag));
			}

			loadDescription(bugCheckerInfo, ruleKey, rule);
		}
	}

	private String getSeverity(BugCheckerInfo bugCheckerInfo) {
		switch (bugCheckerInfo.defaultSeverity()) {
		case ERROR:
			return Severity.MAJOR;
		case WARNING:
			return Severity.MINOR;
		case SUGGESTION:
			return Severity.INFO;
		default:
			throw new IllegalArgumentException("Unexpected severity: " + bugCheckerInfo.defaultSeverity());
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
			try (InputStream in =  resource.openStream(); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				String html = convertMdToHtml(reader);
				rule.setHtmlDescription(html);
			} catch (Exception e) {
				handleDescriptionReadException(ruleName, rule, resource, e);
			}
		} else {
			try (StringReader reader = new StringReader(bugCheckerInfo.message())) {
				String html = convertMdToHtml(reader);
				String link = getBugCheckerLink(bugCheckerInfo);

				html += "\n<b>See: </b><a href=\"" + link + "\" target=\"_blank\">" + link + "</a>";

				rule.setHtmlDescription(html);
			} catch (Exception e) {
				handleDescriptionReadException(ruleName, rule, resource, e);
			}
		}
	}

	public void handleDescriptionReadException(String ruleName, NewRule rule, URL resource, Exception e) {
		LOGGER.warn("Error parsing MD description for {}", ruleName, e);
		rule.setMarkdownDescription(resource);
	}

	public String convertMdToHtml(Reader reader) throws IOException {
		Parser parser = Parser.builder().build();
		HtmlRenderer renderer = HtmlRenderer.builder().build();

		Node node = parser.parseReader(reader);

		return renderer.render(node);
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

	public static String repository(BugCheckerInfo bugCheckerInfo) {
		return repository(bugCheckerInfo.checkerClass());
	}

	public static String repository(Class<? extends BugChecker> type) {
		String className = type.getName();
		if (className.startsWith("com.google.errorprone.")) {
			return ERRORPRONE_REPOSITORY;
		} else if (className.startsWith("com.uber.nullaway.")) {
			return NULLAWAY_REPOSITORY;
		} else if (className.startsWith("jp.skypencil.errorprone.slf4j.")) {
			return ERRORPRONE_SLF4J_REPOSITORY;
		} else if (className.startsWith("autodispose2.")) {
			return AUTODISPOSE2_REPOSITORY;
		} else if (className.startsWith("tech.picnic.errorprone.")) {
			return PICNIC_REPOSITORY;
		} else {
			throw new ErrorAwayException("Could not find rules repository for class " + className);
		}
	}

	/**
	 * Some plugins do not declare their link on the {@link BugPattern} annotation
	 * @param bugCheckerInfo The {@link BugCheckerInfo}
	 * @return The link for the give {@link BugCheckerInfo}
	 */
	private String getBugCheckerLink(BugCheckerInfo bugCheckerInfo) {
		switch (repository(bugCheckerInfo)) {
		case NULLAWAY_REPOSITORY:
			return "https://github.com/uber/NullAway/wiki/Error-Messages";
		case AUTODISPOSE2_REPOSITORY:
			return "https://github.com/uber/AutoDispose/wiki/Error-Prone-Checker";
		default:
			return bugCheckerInfo.linkUrl();
		}
	}
}
