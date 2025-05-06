/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.integration.router.config;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 */
public class SplitterParserTests {

	@Test
	public void splitterAdapterWithRefAndMethod() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("splitterAdapterWithRefAndMethodInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new GenericMessage<String>("this.is.a.test"));
		Message<?> result1 = output.receive(1000);
		assertThat(result1.getPayload()).isEqualTo("this");
		Message<?> result2 = output.receive(1000);
		assertThat(result2.getPayload()).isEqualTo("is");
		Message<?> result3 = output.receive(1000);
		assertThat(result3.getPayload()).isEqualTo("a");
		Message<?> result4 = output.receive(1000);
		assertThat(result4.getPayload()).isEqualTo("test");
		assertThat(output.receive(0)).isNull();
		context.close();
	}

	@Test
	public void splitterAdapterWithRefOnly() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("splitterAdapterWithRefOnlyInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new GenericMessage<String>("this.is.a.test"));
		Message<?> result1 = output.receive(1000);
		assertThat(result1.getPayload()).isEqualTo("this");
		Message<?> result2 = output.receive(1000);
		assertThat(result2.getPayload()).isEqualTo("is");
		Message<?> result3 = output.receive(1000);
		assertThat(result3.getPayload()).isEqualTo("a");
		Message<?> result4 = output.receive(1000);
		assertThat(result4.getPayload()).isEqualTo("test");
		assertThat(output.receive(0)).isNull();
		context.close();
	}

	@Test
	public void splitterImplementation() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("splitterImplementationInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new GenericMessage<String>("this.is.a.test"));
		Message<?> result1 = output.receive(1000);
		assertThat(result1.getPayload()).isEqualTo("this");
		Message<?> result2 = output.receive(1000);
		assertThat(result2.getPayload()).isEqualTo("is");
		Message<?> result3 = output.receive(1000);
		assertThat(result3.getPayload()).isEqualTo("a");
		Message<?> result4 = output.receive(1000);
		assertThat(result4.getPayload()).isEqualTo("test");
		assertThat(output.receive(0)).isNull();
		context.close();
	}

	@Test
	public void splitterParserTestWithRequiresReply() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		DirectChannel inputChannel = context.getBean("requiresReplyInput", DirectChannel.class);
		assertThatThrownBy(() -> inputChannel.send(MessageBuilder.withPayload(Collections.emptyList()).build()))
				.isInstanceOf(ReplyRequiredException.class);
		context.close();
	}

	@Test
	public void splitterParserTestApplySequenceFalse() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		DirectChannel inputChannel = context.getBean("noSequenceInput", DirectChannel.class);
		PollableChannel output = (PollableChannel) context.getBean("output");
		inputChannel.send(MessageBuilder.withPayload(Collections.emptyList()).build());
		Message<?> message = output.receive(1000);
		assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceNumber()).isEqualTo(0);
		assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceSize()).isEqualTo(0);
		context.close();
	}

}
