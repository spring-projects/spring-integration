/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class GatewayInvokingMessageHandlerTests {

	@Autowired
	@Qualifier("inputA")
	SubscribableChannel channel;

	@Autowired
	@Qualifier("simpleGateway")
	SimpleGateway gateway;

	@Autowired
	@Qualifier("gatewayWithError")
	SimpleGateway gatewayWithError;

	@Autowired
	@Qualifier("gatewayWithErrorAsync")
	SimpleGateway gatewayWithErrorAsync;

	@Autowired
	@Qualifier("gatewayWithErrorChannelAndTransformer")
	SimpleGateway gatewayWithErrorChannelAndTransformer;

	@Autowired
	@Qualifier("inputB")
	SubscribableChannel output;

	@Test
	public void validateGatewayInTheChainViaChannel() {
		output.subscribe(message -> {
			assertThat(message.getPayload()).isEqualTo("echo:echo:echo:hello");
			assertThat(message.getHeaders().get("foo")).isEqualTo("foo");
			assertThat(message.getHeaders().get("name")).isEqualTo("oleg");
		});
		channel.send(new GenericMessage<String>("hello"));
	}

	@Test
	public void validateGatewayInTheChainViaAnotherGateway() {
		output.subscribe(message -> {
			assertThat(message.getPayload()).isEqualTo("echo:echo:echo:hello");
			assertThat(message.getHeaders().get("foo")).isEqualTo("foo");
			assertThat(message.getHeaders().get("name")).isEqualTo("oleg");
		});
		String result = gateway.process("hello");
		assertThat(result).isEqualTo("echo:echo:echo:hello");
	}

	@Test
	public void validateGatewayWithErrorMessageReturned() {
		String result = gatewayWithErrorChannelAndTransformer.process("echoWithRuntimeExceptionChannel");
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("Error happened in message: echoWithRuntimeExceptionChannel");

		assertThatExceptionOfType(SampleRuntimeException.class)
				.isThrownBy(() -> gatewayWithError.process("echoWithRuntimeExceptionChannel"))
				.withMessage("echoWithRuntimeExceptionChannel");

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> gatewayWithError.process("echoWithMessagingExceptionChannel"))
				.extracting("failedMessage.payload")
				.isEqualTo("echoWithMessagingExceptionChannel");

		result = gatewayWithErrorChannelAndTransformer.process("echoWithMessagingExceptionChannel");
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("Error happened in message: echoWithMessagingExceptionChannel");
	}

	@Test
	public void validateGatewayWithErrorAsync() {
		assertThatExceptionOfType(SampleRuntimeException.class)
				.isThrownBy(() -> gatewayWithErrorAsync.process("echoWithErrorAsyncChannel"));
	}

	@Test
	public void validateGatewayWithErrorFlowReturningMessage() {
		Object result = gatewayWithErrorChannelAndTransformer.process("echoWithErrorAsyncChannel");
		assertThat(result).isEqualTo("Error happened in message: echoWithErrorAsyncChannel");
	}

	public static class SampleErrorTransformer {

		public Message<?> toMessage(Throwable object) {
			MessageHandlingException ex = (MessageHandlingException) object;
			return MessageBuilder.withPayload("Error happened in message: " + ex.getFailedMessage().getPayload())
					.build();
		}

	}

	public interface SimpleGateway {

		String process(String str);

	}

	public static class SimpleService {

		public String echo(String value) {
			return "echo:" + value;
		}

		public String echoWithRuntimeException(String value) {
			throw new SampleRuntimeException(value);
		}

		public String echoWithMessagingException(String value) {
			throw new MessageHandlingException(new GenericMessage<>(value));
		}

		public String echoWithErrorAsync(String value) {
			throw new SampleRuntimeException(value);
		}

	}

	@SuppressWarnings("serial")
	public static class SampleCheckedException extends Exception {

		public SampleCheckedException(String message) {
			super(message);
		}

	}

	@SuppressWarnings("serial")
	public static class SampleRuntimeException extends RuntimeException {

		public SampleRuntimeException(String message) {
			super(message);
		}

	}

}
