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

import org.springframework.messaging.Message;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

/**
 * A convenient factory for {@link IntegrationObservation} instances.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public final class IntegrationObservations {

	private static final DefaultMessageReceiverObservationConvention DEFAULT_MESSAGE_RECEIVER_OBSERVATION_CONVENTION =
			new DefaultMessageReceiverObservationConvention();

	/**
	 * The factory method for the {@link IntegrationObservation#HANDLER}.
	 * @param observationRegistry the {@link ObservationRegistry} to use.
	 * @param message the {@link Message} as a context for observation.
	 * @param handlerName the name of message handler as a context value.
	 * @return an {@link Observation} to observe a message handling.
	 */
	public static Observation handlerObservation(ObservationRegistry observationRegistry, Message<?> message,
			String handlerName) {

		return IntegrationObservation.HANDLER.observation(null, DEFAULT_MESSAGE_RECEIVER_OBSERVATION_CONVENTION,
				new MessageReceiverContext(message, handlerName), observationRegistry);
	}

	private IntegrationObservations() {
	}

}
