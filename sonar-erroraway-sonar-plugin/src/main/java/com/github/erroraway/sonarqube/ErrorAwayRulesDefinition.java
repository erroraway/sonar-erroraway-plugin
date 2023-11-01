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

import static com.github.erroraway.rules.ErrorAwayRulesMapping.AUTODISPOSE2_REPOSITORY;
import static com.github.erroraway.rules.ErrorAwayRulesMapping.ERRORPRONE_REPOSITORY;
import static com.github.erroraway.rules.ErrorAwayRulesMapping.ERRORPRONE_SLF4J_REPOSITORY;
import static com.github.erroraway.rules.ErrorAwayRulesMapping.NULLAWAY_REPOSITORY;
import static com.github.erroraway.rules.ErrorAwayRulesMapping.PICNIC_REPOSITORY;
import static com.github.erroraway.rules.ErrorAwayRulesMapping.repository;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

import com.github.erroraway.ErrorAwayException;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * @author Guillaume Toison
 *
 */
public class ErrorAwayRulesDefinition implements RulesDefinition {
	private static final String RESOURCE_FOLDER = "com/github/erroraway/rules";

	@Override
	public void define(Context context) {
		loadRepository(context, ERRORPRONE_REPOSITORY, "Error Prone");
		loadRepository(context, NULLAWAY_REPOSITORY, "Null Away");
		loadRepository(context, ERRORPRONE_SLF4J_REPOSITORY, "Error Prone SLF4J");
		loadRepository(context, AUTODISPOSE2_REPOSITORY, "AutoDispose");
		loadRepository(context, PICNIC_REPOSITORY, "Picnic Error Prone Support");
	}
	
	public void loadRepository(Context context, String repository, String repositoryName) {
		String resourceFolder = resourceFolder(repository);
		NewRepository errorProneRepository = context.createRepository(repository, "java").setName(repositoryName);
		
		List<String> ruleKeys = loadRepositoryRuleKeys(resourceFolder);
		
		RuleMetadataLoader loader = new RuleMetadataLoader(resourceFolder);
		loader.addRulesByRuleKey(errorProneRepository, ruleKeys);
		
		errorProneRepository.done();
	}

	public String resourceFolder(String repository) {
		return RESOURCE_FOLDER + '/' + repository;
	}

	public List<String> loadRepositoryRuleKeys(String resourceFolder) {
		try {
			JsonParser parser = new JsonParser();
			JsonElement repositoryMetaData = parser.parse(toString(resourceFolder + "/repository.json", UTF_8));
			JsonArray rules = repositoryMetaData.getAsJsonObject().get("rules").getAsJsonArray();
			List<String> ruleKeys = new ArrayList<>();
			
			for (int i = 0; i < rules.size(); i++) {
				ruleKeys.add(rules.get(i).getAsString());
			}
			
			return ruleKeys;
		} catch (Exception e) {
			throw new ErrorAwayException("Error loading repository metadata from " + resourceFolder, e);
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
	
	private String toString(String path, Charset charset) throws IOException {
		try (InputStream input = ErrorAwayRulesDefinition.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) {
				throw new IOException("Resource not found in the classpath: " + path);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			for (int read = input.read(buffer); read != -1; read = input.read(buffer)) {
				out.write(buffer, 0, read);
			}
			return new String(out.toByteArray(), charset);
		}
	}
}
