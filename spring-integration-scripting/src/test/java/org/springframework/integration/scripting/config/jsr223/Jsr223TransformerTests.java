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

package org.springframework.integration.scripting.config.jsr223;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
public class Jsr223TransformerTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private MessageChannel inlineScriptInput;

	@Autowired
	private MessageChannel int3162InputChannel;

	@Autowired
	private PollableChannel int3162OutputChannel;

	@Test
	public void referencedScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		for (int i = 1; i <= 3; i++) {
			Message<?> message = MessageBuilder.withPayload("test-" + i).setReplyChannel(replyChannel).build();
			this.referencedScriptInput.send(message);
		}
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("ruby-test-1");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("ruby-test-2");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("ruby-test-3");
		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void inlineScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		for (int i = 1; i <= 3; i++) {
			Message<?> message = MessageBuilder.withPayload("test-" + i).setReplyChannel(replyChannel).build();
			this.inlineScriptInput.send(message);
		}
		assertThat(replyChannel.receive(0).getPayload().toString()).isEqualTo("inline-test-1");
		assertThat(replyChannel.receive(0).getPayload().toString()).isEqualTo("inline-test-2");
		assertThat(replyChannel.receive(0).getPayload().toString()).isEqualTo("inline-test-3");
		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void testInt3162ScriptExecutorThreadSafety() {
		for (int i = 0; i < 100; i++) {
			this.int3162InputChannel.send(new GenericMessage<Object>(i));
		}

		Set<Object> result = new HashSet<>();

		for (int i = 0; i < 100; i++) {
			Message<?> message = this.int3162OutputChannel.receive(10000);
			result.add(message.getPayload());
		}

		assertThat(result.size()).isEqualTo(100);

	}

}
