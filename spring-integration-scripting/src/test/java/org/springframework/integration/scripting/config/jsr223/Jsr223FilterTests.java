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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class Jsr223FilterTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private MessageChannel inlineScriptInput;

	@Test
	public void referencedScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		Message<?> message1 = MessageBuilder.withPayload("test-1")
				.setReplyChannel(replyChannel)
				.setHeader("type", "bad")
				.build();
		Message<?> message2 = MessageBuilder.withPayload("test-2")
				.setReplyChannel(replyChannel)
				.setHeader("type", "good")
				.build();
		this.referencedScriptInput.send(message1);
		this.referencedScriptInput.send(message2);
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("test-2");
		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void inlineScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		Message<?> message1 = MessageBuilder.withPayload("bad").setReplyChannel(replyChannel).build();
		Message<?> message2 = MessageBuilder.withPayload("good").setReplyChannel(replyChannel).build();
		this.inlineScriptInput.send(message1);
		this.inlineScriptInput.send(message2);
		Message<?> received = replyChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("good");
		assertThat(received).isEqualTo(message2);
		assertThat(replyChannel.receive(0)).isNull();
	}

}
