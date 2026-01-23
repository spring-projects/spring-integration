/*
 * Copyright 2019-present the original author or authors.
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

package org.springframework.integration.dsl.gateway;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.gateway.MethodArgsHolder;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 5.1.3
 */
@SpringJUnitConfig
@DirtiesContext
public class GatewayDslTests {

	@Autowired
	@Qualifier("gatewayInput")
	private MessageChannel gatewayInput;

	@Autowired
	@Qualifier("gatewayError")
	private PollableChannel gatewayError;

	@Test
	void testGatewayFlow() {
		PollableChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();

		this.gatewayInput.send(message);

		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("From Gateway SubFlow: FOO");
		assertThat(this.gatewayError.receive(1)).isNull();

		Message<String> otherMessage = MessageBuilder.withPayload("bar").setReplyChannel(replyChannel).build();

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.gatewayInput.send(otherMessage))
				.withCauseExactlyInstanceOf(MessageTimeoutException.class);

		assertThat(replyChannel.receive(1)).isNull();

		receive = this.gatewayError.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive).isInstanceOf(ErrorMessage.class);
		assertThat(receive.getPayload()).isInstanceOf(MessageRejectedException.class);
		String exceptionMessage = ((Exception) receive.getPayload()).getMessage();
		assertThat(exceptionMessage)
				.contains("message has been rejected in filter")
				.contains("from source: 'public org.springframework.integration.dsl.IntegrationFlow " +
						"org.springframework.integration.dsl.gateway.GatewayDslTests$ContextConfiguration.gatewayRequestFlow()'");
	}

	@Autowired
	@Qualifier("nestedGatewayErrorPropagationFlow.input")
	private MessageChannel nestedGatewayErrorPropagationFlowInput;

	@Test
	void testNestedGatewayErrorPropagation() {
		assertThatExceptionOfType(RuntimeException.class)
				.isThrownBy(() -> this.nestedGatewayErrorPropagationFlowInput.send(new GenericMessage<>("test")))
				.withStackTraceContaining("intentional");
	}

	@Autowired
	private MessageFunction functionGateway;

	@Autowired
	@Qualifier("&functionGateway.gateway")
	private GatewayProxyFactoryBean<?> functionGatewayFactoryBean;

	@Test
	void testHeadersFromFunctionGateway() {
		Object payload = this.functionGateway
				.andThen(message -> {
					assertThat(message.getHeaders()).containsKeys("gatewayMethod", "gatewayArgs");
					return message.getPayload();
				})
				.apply("testPayload");

		assertThat(payload).isEqualTo("testPayload");

		Map<Method, MessagingGatewaySupport> gateways = this.functionGatewayFactoryBean.getGateways();
		assertThat(gateways).hasSize(2);

		List<String> methodNames = gateways.keySet().stream().map(Method::getName).collect(Collectors.toList());
		assertThat(methodNames).containsExactlyInAnyOrder("apply", "defaultMethodGateway");

		String defaultMethodPayload = "defaultMethodPayload";
		this.functionGateway.defaultMethodGateway(defaultMethodPayload);

		Message<?> receive = this.gatewayError.receive(10_000);
		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo(defaultMethodPayload);

		MessagingGatewaySupport methodGateway = gateways.values().iterator().next();
		MessagingTemplate messagingTemplate =
				TestUtils.<MessagingTemplate>getPropertyValue(methodGateway, "messagingTemplate");

		assertThat(messagingTemplate.getReceiveTimeout()).isEqualTo(10);
		assertThat(messagingTemplate.getSendTimeout()).isEqualTo(20);
	}

	@Autowired
	private RoutingGateway routingGateway;

	@Autowired
	@Qualifier("&routingGateway")
	private GatewayProxyFactoryBean<?> routingGatewayProxy;

	@Test
	void testRoutingGateway() {
		String result = this.routingGateway.route1("test1");
		assertThat(result).isEqualTo("route1");
		result = this.routingGateway.route2("test2");
		assertThat(result).isEqualTo("route2");
		MessagingGatewaySupport gatewayMethod = this.routingGatewayProxy.getGateways().values().iterator().next();
		assertThat(gatewayMethod.getComponentName())
				.isEqualTo("routingGateway#route1(Object)");
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow gatewayFlow() {
			return IntegrationFlow.from("gatewayInput")
					.gateway("gatewayRequest",
							g -> g.errorChannel("gatewayError").replyTimeout(10L).errorOnTimeout(true))
					.gateway((f) -> f.transform("From Gateway SubFlow: "::concat))
					.get();
		}

		@Bean
		public IntegrationFlow gatewayRequestFlow() {
			return IntegrationFlow.from("gatewayRequest")
					.filter("foo"::equals, (f) -> f.throwExceptionOnRejection(true))
					.<String, String>transform(String::toUpperCase)
					.get();
		}

		@Bean
		public MessageChannel gatewayError() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow nestedGatewayErrorPropagationFlow(TaskExecutor taskExecutor) {
			return f -> f
					.gateway((gatewayFlow) -> gatewayFlow
							.channel((c) -> c.executor(taskExecutor))
							.gateway((nestedGatewayFlow) -> nestedGatewayFlow
									.transform((m) -> {
										throw new RuntimeException("intentional");
									})));
		}

		@Bean
		public IntegrationFlow functionGateway() {
			return IntegrationFlow.from(MessageFunction.class,
							(gateway) -> gateway
									.header("gatewayMethod", MethodArgsHolder::method)
									.header("gatewayArgs", MethodArgsHolder::args)
									.replyTimeout(10)
									.requestTimeout(20))
					.bridge()
					.get();
		}

		@Bean
		public IntegrationFlow routingGatewayFlow() {
			return IntegrationFlow.from(RoutingGateway.class,
							(gateway) -> gateway.beanName("routingGateway").header("gatewayMethod", MethodArgsHolder::method))
					.route(Message.class, (message) ->
									message.getHeaders().get("gatewayMethod", Method.class).getName(),
							(router) -> router
									.subFlowMapping("route1", (subFlow) -> subFlow.transform((payload) -> "route1"))
									.subFlowMapping("route2", (subFlow) -> subFlow.transform((payload) -> "route2")))
					.get();
		}

	}

	interface MessageFunction {

		Message<?> apply(Object t);

		@Gateway(requestChannel = "gatewayError")
		default void defaultMethodGateway(Object payload) {
			throw new UnsupportedOperationException();
		}

		default <V> Function<Object, V> andThen(Function<? super Message<?>, ? extends V> after) {
			Objects.requireNonNull(after);
			return (t) -> after.apply(apply(t));
		}

		static <T> Function<T, T> identity() {
			return t -> t;
		}

	}

	interface RoutingGateway {

		String route1(Object payload);

		String route2(Object payload);

	}

}
