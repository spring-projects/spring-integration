/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Enterprise Integration Pattern types.
 * Used to indicate which pattern a target component implements.
 *
 * @author Artem Bilan
 *
 * @since 5.2.1
 */
public enum IntegrationPatternType {

	service_activator(IntegrationPatternCategory.messaging_endpoint),

	splitter(IntegrationPatternCategory.message_routing),

	transformer(IntegrationPatternCategory.message_transformation),

	header_enricher(IntegrationPatternCategory.message_transformation),

	filter(IntegrationPatternCategory.message_routing);

	private final IntegrationPatternCategory patternCategory;

	IntegrationPatternType(IntegrationPatternCategory patternCategory) {
		this.patternCategory = patternCategory;
	}

	public IntegrationPatternCategory getPatternCategory() {
		return this.patternCategory;
	}

	/**
	 * The Enterprise Integration Pattern categories.
	 * Used to indicate which pattern category a target component belongs.
	 */
	public enum IntegrationPatternCategory {

		messaging_endpoint(service_activator),

		message_routing(
				splitter,
				filter),

		message_transformation(
				transformer,
				header_enricher);

		private final IntegrationPatternType[] patternTypes;

		IntegrationPatternCategory(IntegrationPatternType... patternTypes) {
			this.patternTypes = patternTypes;
		}

		public Set<IntegrationPatternType> getPatternTypes() {
			return Arrays.stream(this.patternTypes).collect(Collectors.toSet());
		}

	}

}
