/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.support.management.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.docs.DocumentedObservation;

/**
 * The {@link DocumentedObservation} implementation for Spring Integration infrastructure.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public enum IntegrationObservation implements DocumentedObservation {

	/**
	 * Observation for message handlers.
	 */
	HANDLER {
		@Override
		public String getPrefix() {
			return "spring.integration.";
		}

		@Override
		public Class<DefaultMessageReceiverObservationConvention> getDefaultConvention() {
			return DefaultMessageReceiverObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return HandlerTags.values();
		}

	};

	/**
	 * Key names for message handler observations.
	 */
	public enum HandlerTags implements KeyName {

		/**
		 * Name of the message handler component.
		 */
		COMPONENT_NAME {
			@Override
			public String asString() {
				return "spring.integration.name";
			}

		},

		/**
		 * Type of the component - 'handler'.
		 */
		COMPONENT_TYPE {
			@Override
			public String asString() {
				return "spring.integration.type";
			}

		}

	}

}
