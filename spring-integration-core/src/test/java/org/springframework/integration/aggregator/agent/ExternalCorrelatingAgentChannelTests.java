/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.aggregator.agent;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for an externally hosted correlating agent and dependency gateway.
 *
 * @author OpenAI
 *
 * @since 7.2
 */
class ExternalCorrelatingAgentChannelTests {

	@Test
	void externallyHostedAgentUsesExportedDependencyPort() throws Exception {
		String dependencyServerName = InProcessServerBuilder.generateName();
		String agentServerName = InProcessServerBuilder.generateName();
		ManagedChannel dependencyChannel = InProcessChannelBuilder.forName(dependencyServerName).directExecutor().build();
		ManagedChannel agentChannel = InProcessChannelBuilder.forName(agentServerName).directExecutor().build();
		AggregatingMessageHandler handler =
				new AggregatingMessageHandler(new DefaultAggregatingMessageGroupProcessor());
		handler.setCorrelatingAgentChannel(agentChannel);

		Server dependencyServer = InProcessServerBuilder.forName(dependencyServerName)
				.directExecutor()
				.addService(handler.getCorrelatingDependencyPort())
				.build()
				.start();
		Server agentServer = InProcessServerBuilder.forName(agentServerName)
				.directExecutor()
				.addService(new EmbabelCorrelatingAgentService(dependencyChannel))
				.build()
				.start();

		try {
			QueueChannel replies = new QueueChannel();
			handler.handleMessage(message("one", 1, replies));
			handler.handleMessage(message("two", 2, replies));

			Message<?> result = replies.receive(0);
			assertThat(result).isNotNull();
			assertThat(result.getPayload()).isEqualTo(List.of("one", "two"));
		}
		finally {
			handler.destroy();
			agentServer.shutdownNow();
			dependencyServer.shutdownNow();
			agentChannel.shutdownNow();
			dependencyChannel.shutdownNow();
		}
	}

	private static Message<String> message(String payload, int sequenceNumber, QueueChannel replyChannel) {
		return MessageBuilder.withPayload(payload)
				.setCorrelationId("external-agent")
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(2)
				.setReplyChannel(replyChannel)
				.build();
	}

}
