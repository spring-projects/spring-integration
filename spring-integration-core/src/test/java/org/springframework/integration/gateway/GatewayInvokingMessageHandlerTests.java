/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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

		try {
			gatewayWithError.process("echoWithRuntimeExceptionChannel");
			fail("SampleRuntimeException expected");
		}
		catch (SampleRuntimeException e) {
			assertThat(e.getMessage()).isEqualTo("echoWithRuntimeExceptionChannel");
		}

		try {
			gatewayWithError.process("echoWithMessagingExceptionChannel");
			fail("MessageHandlingException expected");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getFailedMessage().getPayload()).isEqualTo("echoWithMessagingExceptionChannel");
		}

		result = gatewayWithErrorChannelAndTransformer.process("echoWithMessagingExceptionChannel");
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("Error happened in message: echoWithMessagingExceptionChannel");
	}

	@Test
	public void validateGatewayWithErrorAsync() {
		try {
			gatewayWithErrorAsync.process("echoWithErrorAsyncChannel");
			fail("SampleRuntimeException expected");
		}
		catch (Exception e) {
			assertThat(e.getClass()).isEqualTo(SampleRuntimeException.class);
		}
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
