/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.integration.camel.outbound;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.camel.support.CamelHeaderMapper;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class CamelMessageHandlerTests extends CamelTestSupport {

	@Test
	void inOnlyPatternSyncMessageHandler() throws InterruptedException {
		Message<String> messageUnderTest = new GenericMessage<>("Hello Camel!");
		Message<String> messageUnderTest2 = new GenericMessage<>("Hello Camel again!");

		MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
		mockEndpoint.message(0).body().isEqualTo(messageUnderTest.getPayload());
		mockEndpoint.message(0).header(MessageHeaders.ID).isEqualTo(messageUnderTest.getHeaders().getId());
		mockEndpoint.message(1).body().isEqualTo(messageUnderTest2.getPayload());
		mockEndpoint.message(1).header(MessageHeaders.ID).isEqualTo(messageUnderTest2.getHeaders().getId());

		CamelMessageHandler camelMessageHandler = new CamelMessageHandler(template());
		camelMessageHandler.setEndpointUri("direct:simple");
		camelMessageHandler.setBeanFactory(mock(BeanFactory.class));
		camelMessageHandler.afterPropertiesSet();

		camelMessageHandler.handleMessage(messageUnderTest);
		camelMessageHandler.handleMessage(messageUnderTest2);

		mockEndpoint.assertIsSatisfied();
	}

	@Test
	void inOutPatternSyncMessageHandlerWithNoRequestHeadersButReplyHeaders() throws InterruptedException {
		SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
		QueueChannel replyChannel = new QueueChannel();
		Message<String> messageUnderTest =
				MessageBuilder.withPayload("test data")
						.setHeader("exchangePattern", "InOut")
						.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
						.build();

		MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
		mockEndpoint.expectedHeaderReceived(MessageHeaders.ID, null);
		mockEndpoint.expectedHeaderReceived(MessageHeaders.TIMESTAMP, null);
		mockEndpoint.whenAnyExchangeReceived(exchange -> {
			org.apache.camel.Message out = exchange.getMessage();
			out.setBody("Reply for: " + exchange.getIn().getBody());
			out.setHeader("testHeader", "testHeaderValue");
			out.setHeader("notMappedHeader", "someValue");
		});

		CamelHeaderMapper headerMapper = new CamelHeaderMapper();
		headerMapper.setOutboundHeaderNames("");
		headerMapper.setInboundHeaderNames("testHeader");

		CamelMessageHandler camelMessageHandler = new CamelMessageHandler(template());
		camelMessageHandler.setEndpointUriExpression(new FunctionExpression<>(m -> "direct:simple"));
		camelMessageHandler.setExchangePatternExpression(spelExpressionParser.parseExpression("headers.exchangePattern"));
		camelMessageHandler.setHeaderMapper(headerMapper);
		camelMessageHandler.setBeanFactory(mock(BeanFactory.class));
		camelMessageHandler.afterPropertiesSet();

		camelMessageHandler.handleMessage(messageUnderTest);

		Message<?> receive = replyChannel.receive(10_000);

		mockEndpoint.assertIsSatisfied();

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("Reply for: test data");
		assertThat(receive.getHeaders())
				.containsEntry("testHeader", "testHeaderValue")
				.doesNotContainKey("notMappedHeader");
	}

	@Test
	void inOnlyPatternAsyncMessageHandlerWithException() throws InterruptedException {
		QueueChannel errorChannel = new QueueChannel();
		Message<String> messageUnderTest =
				MessageBuilder.withPayload("test data")
						.setHeader(MessageHeaders.ERROR_CHANNEL, errorChannel)
						.build();

		MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
		mockEndpoint.whenAnyExchangeReceived(exchange -> {
			throw new RuntimeException("intentional");
		});

		CamelMessageHandler camelMessageHandler = new CamelMessageHandler(template());
		camelMessageHandler.setEndpointUri("direct:simple");
		camelMessageHandler.setBeanFactory(mock(BeanFactory.class));
		camelMessageHandler.setAsync(true);
		camelMessageHandler.afterPropertiesSet();

		camelMessageHandler.handleMessage(messageUnderTest);
		Message<?> receive = errorChannel.receive(10_000);

		mockEndpoint.assertIsSatisfied();

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload())
				.asInstanceOf(InstanceOfAssertFactories.throwable(MessageHandlingException.class))
				.hasCauseInstanceOf(CamelExecutionException.class)
				.hasRootCauseExactlyInstanceOf(RuntimeException.class)
				.hasStackTraceContaining("intentional");
	}

	@Test
	void inOutPatternAsyncMessageHandler() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<String> messageUnderTest =
				MessageBuilder.withPayload("test async data")
						.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
						.build();

		MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
		mockEndpoint.whenAnyExchangeReceived(exchange ->
				exchange.getMessage().setBody("Async reply for: " + exchange.getIn().getBody()));

		ProducerTemplate producerTemplate = template();
		producerTemplate.setDefaultEndpointUri("direct:simple");
		CamelMessageHandler camelMessageHandler = new CamelMessageHandler(producerTemplate);
		camelMessageHandler.setExchangePattern(ExchangePattern.InOut);
		camelMessageHandler.setBeanFactory(mock(BeanFactory.class));
		camelMessageHandler.setAsync(true);
		camelMessageHandler.afterPropertiesSet();

		camelMessageHandler.handleMessage(messageUnderTest);

		Message<?> receive = replyChannel.receive(10_000);

		mockEndpoint.assertIsSatisfied();

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("Async reply for: test async data");
	}

	@Override
	protected RoutesBuilder createRouteBuilder() {
		return new RouteBuilder() {

			@Override
			public void configure() {
				from("direct:simple").to("mock:result");
			}

		};
	}

}
