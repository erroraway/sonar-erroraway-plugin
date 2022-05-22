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

/**
 * @author Guillaume
 *
 */
public enum NullAwayOption {
	ANNOTATED_PACKAGES(
			"AnnotatedPackages",
			"nullaway.annotated.packages",
			"Annotated Packages",
			"The list of annotated packages for NullAway"),
	UNANNOTATED_PACKAGES(
			"UnannotatedSubPackages",
			"nullaway.unannotated.packages",
			"Annotated Packages",
			"The list of unannotated packages to be excluded from the annotated package list for NullAway"),
	UNANNOTATED_CLASSES(
			"UnannotatedSubPackages",
			"nullaway.unannotated.classes",
			"Annotated Classes",
			"The list of classes within annotated packages to be treated as unannotated for NullAway"),
	KNOWN_INITIALIZERS(
			"KnownInitializers",
			"nullaway.known.initializers",
			"Known Initializers",
			"The fully qualified nam of method that NullAway should treat as initializers"),
	EXCLUDED_FIELD_ANNOTATIONS(
			"ExcludedFieldAnnotations",
			"nullaway.field.annotations",
			"Excluded Field Annotations",
			"A list of annotations that cause fields to be excluded from being checked for proper initialization"),

	;

	private final String errorproneOption;
	private final String key;
	private final String name;
	private String description;

	private NullAwayOption(String errorproneOption, String key, String name, String description) {
		this.errorproneOption = errorproneOption;
		this.key = key;
		this.name = name;
		this.description = description;
	}

	public String getErrorproneOption() {
		return errorproneOption;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
}
