package org.springframework.integration.graphql.outbound;

import graphql.ExecutionResult;
import org.springframework.graphql.GraphQlService;
import org.springframework.graphql.RequestInput;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
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
