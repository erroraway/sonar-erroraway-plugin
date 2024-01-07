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
package com.github.erroraway.rules;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import com.github.erroraway.ErrorAwayException;
import com.google.errorprone.bugpatterns.BugChecker;

/**
 * @author Guillaume Toison
 *
 */
public final class ErrorAwayRulesMapping {
	public static final String ERRORPRONE_REPOSITORY = "errorprone";
	public static final String NULLAWAY_REPOSITORY = "nullaway";
	public static final String ERRORPRONE_SLF4J_REPOSITORY = "errorprone-slf4j";
	public static final String PICNIC_REPOSITORY = "picnic-errorprone";

	public static final int ERRORPRONE_REPOSITORY_RULES_COUNT = 434;
	public static final int NULLAWAY_REPOSITORY_RULES_COUNT = 1;
	public static final int ERRORPRONE_SLF4J_REPOSITORY_RULES_COUNT = 8;
	public static final int PICNIC_REPOSITORY_RULES_COUNT = 39;

	public static final int RULES_COUNT = ERRORPRONE_REPOSITORY_RULES_COUNT 
			+ NULLAWAY_REPOSITORY_RULES_COUNT 
			+ ERRORPRONE_SLF4J_REPOSITORY_RULES_COUNT
			+ PICNIC_REPOSITORY_RULES_COUNT;

	public static final List<String> REPOSITORIES = Collections.unmodifiableList(Arrays.asList( 
			ERRORPRONE_REPOSITORY, 
			NULLAWAY_REPOSITORY, 
			ERRORPRONE_SLF4J_REPOSITORY, 
			PICNIC_REPOSITORY));
	
	private ErrorAwayRulesMapping() {
	}

	public static String repository(Class<? extends BugChecker> type) {
		String className = type.getName();
		if (className.startsWith("com.google.errorprone.")) {
			return ERRORPRONE_REPOSITORY;
		} else if (className.startsWith("com.uber.nullaway.")) {
			return NULLAWAY_REPOSITORY;
		} else if (className.startsWith("jp.skypencil.errorprone.slf4j.")) {
			return ERRORPRONE_SLF4J_REPOSITORY;
		} else if (className.startsWith("tech.picnic.errorprone.")) {
			return PICNIC_REPOSITORY;
		} else {
			throw new ErrorAwayException("Could not find rules repository for class " + className);
		}
	}
	
	public static Iterator<BugChecker> pluginCheckers() {
		ClassLoader loader = ErrorAwayRulesMapping.class.getClassLoader();

		return ServiceLoader.load(BugChecker.class, loader).iterator();
	}
}
