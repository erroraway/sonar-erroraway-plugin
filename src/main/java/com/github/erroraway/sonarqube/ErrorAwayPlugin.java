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
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

/**
 * @author Guillaume
 *
 */
public class ErrorAwayPlugin implements Plugin {
	private static final String PROPERTY_NULLAWAY_CATEGORY = "NullAway";
	private static final String PROPERTY_ERRORAWAY_CATEGORY = "ErrorAway";
	private static final String PROPERTY_MAVEN_SUBCATEGORY = "Maven";

	public static final String MAVEN_WORK_OFFLINE = "erroraway.maven.work.offline";
	public static final String MAVEN_USER_SETTINGS_FILE = "erroraway.maven.user.settings.file";
	public static final String MAVEN_LOCAL_REPOSITORY = "erroraway.maven.local.repository";
	public static final String MAVEN_USE_TEMP_LOCAL_REPOSITORY = "erroraway.maven.use.temp.local.repository";
	public static final String MAVEN_REPOSITORIES = "erroraway.maven.repositories";
	public static final String CLASS_PATH_MAVEN_COORDINATES = "erroraway.classpath.maven.coordinates";
	public static final String ANNOTATION_PROCESSORS_MAVEN_COORDINATES = "erroraway.annotation.processors.maven.coordinates";

	@Override
	public void define(Context context) {
		context.addExtension(PropertyDefinition
				.builder(MAVEN_WORK_OFFLINE)
				.name("Maven offline")
				.description("Let Maven work offline")
				.category(PROPERTY_ERRORAWAY_CATEGORY)
				.subCategory(PROPERTY_MAVEN_SUBCATEGORY)
				.onQualifiers(Qualifiers.PROJECT)
				.type(PropertyType.BOOLEAN)
				.build());
		
		context.addExtension(PropertyDefinition
				.builder(MAVEN_USER_SETTINGS_FILE)
				.name("Maven user settings file")
				.description("The maven user settings file, e.g. C:/Users/jdoe/.m2/settings.xml")
				.category(PROPERTY_ERRORAWAY_CATEGORY)
				.subCategory(PROPERTY_MAVEN_SUBCATEGORY)
				.onQualifiers(Qualifiers.PROJECT)
				.build());
		
		context.addExtension(PropertyDefinition
				.builder(MAVEN_LOCAL_REPOSITORY)
				.name("Maven local repository")
				.description("The maven local repository, e.g. C:/Users/jdoe/.m2/repository")
				.category(PROPERTY_ERRORAWAY_CATEGORY)
				.subCategory(PROPERTY_MAVEN_SUBCATEGORY)
				.onQualifiers(Qualifiers.PROJECT)
				.build());
		
		context.addExtension(PropertyDefinition
				.builder(MAVEN_USE_TEMP_LOCAL_REPOSITORY)
				.name("Use Maven temporary local repository")
				.description("Use a temporary folder for the Maven local repository")
				.category(PROPERTY_ERRORAWAY_CATEGORY)
				.subCategory(PROPERTY_MAVEN_SUBCATEGORY)
				.onQualifiers(Qualifiers.PROJECT)
				.type(PropertyType.BOOLEAN)
				.build());
		
		context.addExtension(PropertyDefinition
				.builder(MAVEN_REPOSITORIES)
				.name("Maven repositories")
				.description("The maven remote repositories, e.g. https://repo1.maven.org/maven2/")
				.category(PROPERTY_ERRORAWAY_CATEGORY)
				.subCategory(PROPERTY_MAVEN_SUBCATEGORY)
				.onQualifiers(Qualifiers.PROJECT)
				.multiValues(true)
				.build());
		
		context.addExtension(PropertyDefinition
				.builder(CLASS_PATH_MAVEN_COORDINATES)
				.name("Classpath Maven coordinates")
				.description("The maven coordinates of dependencies required to compile the project, e.g. org.slf4j:slf4j-api:1.7.36")
				.category(PROPERTY_ERRORAWAY_CATEGORY)
				.subCategory(PROPERTY_MAVEN_SUBCATEGORY)
				.onQualifiers(Qualifiers.PROJECT)
				.multiValues(true)
				.build());
		
		context.addExtension(PropertyDefinition
				.builder(ANNOTATION_PROCESSORS_MAVEN_COORDINATES)
				.name("Annotation processors Maven coordinates")
				.description("The maven coordinates of annotation processors, e.g. com.google.auto.value:auto-value:1.9")
				.category(PROPERTY_ERRORAWAY_CATEGORY)
				.subCategory(PROPERTY_MAVEN_SUBCATEGORY)
				.onQualifiers(Qualifiers.PROJECT)
				.multiValues(true)
				.build());
		
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
		context.addExtension(ErrorAwayDependencyManager.class);
	}
}
