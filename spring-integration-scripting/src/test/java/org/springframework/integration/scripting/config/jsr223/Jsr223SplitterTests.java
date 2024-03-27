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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class Jsr223SplitterTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private MessageChannel inlineScriptInput;

	@Test
	public void referencedScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		Message<?> message = MessageBuilder.withPayload("x,y,z").setReplyChannel(replyChannel).build();
		this.referencedScriptInput.send(message);
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("x");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("y");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("z");
		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void inlineScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		Message<?> message = MessageBuilder.withPayload("a   b c").setReplyChannel(replyChannel).build();
		this.inlineScriptInput.send(message);
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("a");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("b");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("c");
		assertThat(replyChannel.receive(0)).isNull();
	}

}
