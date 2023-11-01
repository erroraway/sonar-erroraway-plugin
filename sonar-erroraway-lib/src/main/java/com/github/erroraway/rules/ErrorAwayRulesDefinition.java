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

/**
 * @author Guillaume Toison
 *
 */
public class ErrorAwayRulesDefinition {
	public static final String ERRORPRONE_REPOSITORY = "errorprone";
	public static final String NULLAWAY_REPOSITORY = "nullaway";
	public static final String ERRORPRONE_SLF4J_REPOSITORY = "errorprone-slf4j";
	public static final String AUTODISPOSE2_REPOSITORY = "autodispose2";
	public static final String PICNIC_REPOSITORY = "picnic-errorprone";

	public static final int ERRORPRONE_REPOSITORY_RULES_COUNT = 412;
	public static final int NULLAWAY_REPOSITORY_RULES_COUNT = 1;
	public static final int ERRORPRONE_SLF4J_REPOSITORY_RULES_COUNT = 8;
	public static final int AUTODISPOSE2_REPOSITORY_RULES_COUNT = 1;
	public static final int PICNIC_REPOSITORY_RULES_COUNT = 40;

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
}
