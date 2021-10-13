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

import graphql.ExecutionResult;
import reactor.core.publisher.Mono;

/**
 *
 * @author Daniel Frey
 */
public class GraphqlQueryMessageHandler extends AbstractReplyProducingMessageHandler {

	private GraphQlService graphQlService;

	public GraphqlQueryMessageHandler(final GraphQlService graphQlService) {
		this.graphQlService = graphQlService;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {

		Mono<ExecutionResult> result = this.graphQlService.execute((RequestInput) requestMessage.getPayload());

		return result.block();
//				.map(response -> response)
//				.switchIfEmpty(Mono.empty());
	}
}
