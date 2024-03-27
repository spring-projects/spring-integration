/*
 * Copyright 2019-2024 the original author or authors.
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
 * @since 5.3
 */
public enum IntegrationPatternType { // NOSONAR Initialization circularity is useful for static view

	message_channel(IntegrationPatternCategory.messaging_channel),

	publish_subscribe_channel(IntegrationPatternCategory.messaging_channel),

	executor_channel(IntegrationPatternCategory.messaging_channel),

	pollable_channel(IntegrationPatternCategory.messaging_channel),

	reactive_channel(IntegrationPatternCategory.messaging_channel),

	null_channel(IntegrationPatternCategory.messaging_channel),

	bridge(IntegrationPatternCategory.messaging_endpoint),

	service_activator(IntegrationPatternCategory.messaging_endpoint),

	outbound_channel_adapter(IntegrationPatternCategory.messaging_endpoint),

	inbound_channel_adapter(IntegrationPatternCategory.messaging_endpoint),

	outbound_gateway(IntegrationPatternCategory.messaging_endpoint),

	inbound_gateway(IntegrationPatternCategory.messaging_endpoint),

	splitter(IntegrationPatternCategory.message_routing),

	transformer(IntegrationPatternCategory.message_transformation),

	header_enricher(IntegrationPatternCategory.message_transformation),

	filter(IntegrationPatternCategory.message_routing),

	content_enricher(IntegrationPatternCategory.message_transformation),

	header_filter(IntegrationPatternCategory.message_transformation),

	claim_check_in(IntegrationPatternCategory.message_transformation),

	claim_check_out(IntegrationPatternCategory.message_transformation),

	aggregator(IntegrationPatternCategory.message_routing),

	resequencer(IntegrationPatternCategory.message_routing),

	barrier(IntegrationPatternCategory.message_routing),

	chain(IntegrationPatternCategory.message_routing),

	scatter_gather(IntegrationPatternCategory.message_routing),

	delayer(IntegrationPatternCategory.message_routing),

	control_bus(IntegrationPatternCategory.system_management),

	router(IntegrationPatternCategory.message_routing),

	recipient_list_router(IntegrationPatternCategory.message_routing);

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

		messaging_channel(
				message_channel,
				publish_subscribe_channel,
				executor_channel,
				pollable_channel,
				reactive_channel,
				null_channel),

		messaging_endpoint(
				service_activator,
				outbound_channel_adapter,
				inbound_channel_adapter,
				outbound_gateway,
				inbound_gateway,
				bridge),

		message_routing(
				splitter,
				filter,
				aggregator,
				resequencer,
				barrier,
				chain,
				scatter_gather,
				delayer,
				router,
				recipient_list_router),

		message_transformation(
				transformer,
				header_enricher,
				content_enricher,
				header_filter,
				claim_check_in,
				claim_check_out),

		system_management(control_bus);

		private final IntegrationPatternType[] patternTypes;

		IntegrationPatternCategory(IntegrationPatternType... patternTypes) {
			this.patternTypes = Arrays.copyOf(patternTypes, patternTypes.length);
		}

		public Set<IntegrationPatternType> getPatternTypes() {
			return Arrays.stream(this.patternTypes).collect(Collectors.toSet());
		}

	}

}
