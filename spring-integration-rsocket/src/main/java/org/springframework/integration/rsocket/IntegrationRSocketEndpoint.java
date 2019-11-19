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

package org.springframework.integration.rsocket;

import org.springframework.messaging.ReactiveMessageHandler;

/**
 * A marker {@link ReactiveMessageHandler} extension interface for Spring Integration
 * inbound endpoints.
 * It is used as mapping predicate in the internal RSocket acceptor of the
 * {@link AbstractRSocketConnector}.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see AbstractRSocketConnector
 * @see org.springframework.integration.rsocket.inbound.RSocketInboundGateway
 */
public interface IntegrationRSocketEndpoint extends ReactiveMessageHandler {

	/**
	 * Obtain path patterns this {@link ReactiveMessageHandler} is going to be mapped onto.
	 * @return the path patterns for mapping.
	 */
	String[] getPath();

	/**
	 * Obtain {@link RSocketInteractionModel}s
	 * this {@link ReactiveMessageHandler} is going to be mapped onto.
	 * Defaults to all the {@link RSocketInteractionModel}s.
	 * @return the interaction models for mapping.
	 * @since 5.2.2
	 */
	default RSocketInteractionModel[] getInteractionModels() {
		return RSocketInteractionModel.values();
	}

}
