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

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

/**
 * @author Guillaume
 *
 */
public class ErrorAwayPlugin implements Plugin {
	private static final String PROPERTY_NULLAWAY_CATEGORY = "NullAway";

	@Override
	public void define(Context context) {
		
		for (NullAwayOption option : NullAwayOption.values()) {
			context.addExtension(PropertyDefinition
					.builder(option.getKey())
					.name(option.getName())
					.description(option.getDescription())
					.category(PROPERTY_NULLAWAY_CATEGORY)
					.onQualifiers(Qualifiers.PROJECT)
					.multiValues(true)
					.build());
		}
		
		context.addExtension(ErrorAwayRulesDefinition.class);
		context.addExtension(ErrorAwayQualityProfile.class);
		context.addExtension(ErrorAwaySensor.class);
	}
}
