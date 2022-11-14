/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Yilin Wei
 */
@SpringJUnitConfig
@DirtiesContext
public class AnnotatedEndpointActivationTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	@Qualifier("inputAsync")
	private MessageChannel inputAsync;

	@Autowired
	@Qualifier("outputAsync")
	private PollableChannel outputAsync;

	@Autowired
	private AbstractApplicationContext applicationContext;

	// This has to be static because the MessageBus registers the handler
	// more than once (every time a test instance is created), but only one of
	// them will get the message.
	private static volatile int count = 0;

	@BeforeEach
	public void resetCount() {
		count = 0;
	}

	@Test
	public void sendAndReceive() {
		this.input.send(new GenericMessage<>("foo"));
		Message<?> message = this.output.receive(100);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo: 1");
		assertThat(count).isEqualTo(1);

		assertThat(this.applicationContext.containsBean("annotatedEndpoint.process.serviceActivator")).isTrue();
		assertThat(this.applicationContext.containsBean("annotatedEndpoint2.process.serviceActivator")).isTrue();
	}

	@Test
	public void sendAndReceiveAsync() {
		this.inputAsync.send(new GenericMessage<>("foo"));
		Message<?> message = this.outputAsync.receive(100);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
		assertThat(this.applicationContext.containsBean("annotatedEndpoint3.process.serviceActivator")).isTrue();
	}

	@Test
	public void sendAndReceiveImplicitInputChannel() {
		MessageChannel input = this.applicationContext.getBean("inputImplicit", MessageChannel.class);
		input.send(new GenericMessage<>("foo"));
		Message<?> message = this.output.receive(100);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo: 1");
		assertThat(count).isEqualTo(1);
	}

	@Test
	@DirtiesContext
	public void stopContext() {
		applicationContext.stop();
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.input.send(new GenericMessage<>("foo")));
	}

	@Test
	@DirtiesContext
	public void stopAndRestartContext() {
		applicationContext.stop();
		applicationContext.start();
		this.input.send(new GenericMessage<>("foo"));
		Message<?> message = this.output.receive(100);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo: 1");
		assertThat(count).isEqualTo(1);
	}

	@MessageEndpoint
	private static class AnnotatedEndpoint {

		@ServiceActivator(inputChannel = "input", outputChannel = "output")
		public String process(String message) {
			count++;
			return message + ": " + count;
		}

		@ServiceActivator(inputChannel = "inputImplicit", outputChannel = "output")
		public String processImplicit(String message) {
			count++;
			return message + ": " + count;
		}

	}

	@SuppressWarnings("unused")
	private static class AnnotatedEndpoint2 {

		@ServiceActivator(inputChannel = "input", outputChannel = "output")
		public String process(String message) {
			count++;
			return message + ": " + count;
		}

	}

	@SuppressWarnings("unused")
	private static class AnnotatedEndpoint3 {

		@ServiceActivator(inputChannel = "inputAsync", outputChannel = "outputAsync", async = "true")
		public CompletableFuture<String> process(String message) {
			CompletableFuture<String> future = new CompletableFuture<>();
			future.complete(message);
			return future;
		}

	}

}
