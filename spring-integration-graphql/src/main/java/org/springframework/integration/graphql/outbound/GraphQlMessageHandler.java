/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.graphql.outbound;

import org.springframework.graphql.GraphQlService;
import org.springframework.graphql.RequestInput;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import graphql.ExecutionResult;
import reactor.core.publisher.Mono;

/**
 * A {@link org.springframework.messaging.MessageHandler} capable of fielding GraphQL Query, Mutation and Subscription requests.
 *
 * @author Daniel Frey
 * @since 6.0
 */
public class GraphQlMessageHandler extends AbstractReplyProducingMessageHandler {

	private final GraphQlService graphQlService;

	public GraphQlMessageHandler(final GraphQlService graphQlService) {
		Assert.notNull(graphQlService, "'graphQlService' must not be null");

		this.graphQlService = graphQlService;
		setAsync(true);
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {

		if (requestMessage.getPayload() instanceof RequestInput) {

			Mono<ExecutionResult> result = this.graphQlService
					.execute((RequestInput) requestMessage.getPayload());

			return result;
		}
		else {
			throw new IllegalArgumentException("Message payload needs to be 'org.springframework.graphql.RequestInput'");
		}
	}
}
