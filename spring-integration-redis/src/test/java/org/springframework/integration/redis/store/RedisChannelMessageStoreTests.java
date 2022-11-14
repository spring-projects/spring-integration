/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.redis.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.support.json.JacksonJsonUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 4.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
class RedisChannelMessageStoreTests implements RedisContainerTest {

	@Autowired
	private PollableChannel testChannel1;

	@Autowired
	private PollableChannel testChannel2;

	@Autowired
	private PollableChannel testChannel3;

	@Autowired
	private PollableChannel testChannel4;

	@Autowired
	private RedisChannelMessageStore cms;

	@Autowired
	private RedisChannelMessageStore priorityCms;

	@BeforeEach
	@AfterEach
	void setUpTearDown() {
		this.cms.removeMessageGroup("cms:testChannel1");
		this.cms.removeMessageGroup("cms:testChannel2");
		this.priorityCms.removeMessageGroup("priorityCms:testChannel3");
		this.priorityCms.removeMessageGroup("priorityCms:testChannel4");
	}

	@Test
	void testChannel() {
		for (int i = 0; i < 10; i++) {
			this.testChannel1.send(new GenericMessage<>(i));
		}
		assertThat(this.cms.getMessageGroupCount()).isEqualTo(1);
		assertThat(this.cms.messageGroupSize("cms:testChannel1")).isEqualTo(10);
		assertThat(this.cms.getMessageGroup("cms:testChannel1").size()).isEqualTo(10);
		for (int i = 0; i < 10; i++) {
			this.testChannel2.send(MutableMessageBuilder.withPayload(i).build());
		}
		assertThat(this.cms.getMessageGroupCount()).isEqualTo(2);
		assertThat(this.cms.messageGroupSize("cms:testChannel2")).isEqualTo(10);
		assertThat(this.cms.getMessageGroup("cms:testChannel2").size()).isEqualTo(10);
		assertThat(this.cms.getMessageCountForAllMessageGroups()).isEqualTo(20);
		for (int i = 0; i < 10; i++) {
			Message<?> out = this.testChannel1.receive(0);
			assertThat(out).isInstanceOf(GenericMessage.class);
			assertThat(out.getPayload()).isEqualTo(i);
		}
		assertThat(this.testChannel1.receive(0)).isNull();
		for (int i = 0; i < 10; i++) {
			Message<?> out = this.testChannel2.receive(0);
			assertThat(out.getClass().getName()).isEqualTo("org.springframework.integration.support.MutableMessage");
			assertThat(out.getPayload()).isEqualTo(i);
		}
		assertThat(this.testChannel2.receive(0)).isNull();
		assertThat(this.cms.getMessageGroupCount()).isZero();

		for (int i = 0; i < 10; i++) {
			this.testChannel1.send(new GenericMessage<>(i));
		}
		assertThat(this.cms.getMessageGroupCount()).isEqualTo(1);
		assertThat(this.cms.messageGroupSize("cms:testChannel1")).isEqualTo(10);
		this.cms.removeMessageGroup("cms:testChannel1");
		assertThat(this.cms.getMessageGroupCount()).isZero();
		assertThat(this.cms.messageGroupSize("cms:testChannel1")).isZero();
	}

	@Test
	void testPriority() {
		for (int i = 0; i < 10; i++) {
			this.testChannel3.send(MessageBuilder.withPayload(i).setPriority(i).build());
			//We need unique messages
			this.testChannel3.send(MessageBuilder.withPayload(i).setPriority(i).build());
		}
		this.testChannel3.send(MessageBuilder.withPayload(99).setPriority(199).build());
		this.testChannel3.send(MessageBuilder.withPayload(98).build());
		assertThat(this.priorityCms.getMessageGroupCount()).isEqualTo(1);
		assertThat(this.priorityCms.messageGroupSize("priorityCms:testChannel3")).isEqualTo(22);
		assertThat(this.priorityCms.getMessageCountForAllMessageGroups()).isEqualTo(22);
		assertThat(this.priorityCms.getMessageGroup("priorityCms:testChannel3").size()).isEqualTo(22);
		this.testChannel4.send(MessageBuilder.withPayload(98).build());
		this.testChannel4.send(MessageBuilder.withPayload(99).setPriority(5).build());
		assertThat(this.priorityCms.getMessageGroupCount()).isEqualTo(2);
		assertThat(this.priorityCms.getMessageGroup("priorityCms:testChannel4").size()).isEqualTo(2);
		assertThat(this.priorityCms.messageGroupSize("priorityCms:testChannel4")).isEqualTo(2);
		assertThat(this.priorityCms.getMessageCountForAllMessageGroups()).isEqualTo(24);
		for (int i = 0; i < 10; i++) {
			Message<?> m = this.testChannel3.receive(0);
			assertThat(m).isNotNull();
			assertThat(new IntegrationMessageHeaderAccessor(m).getPriority()).isEqualTo(Integer.valueOf(9 - i));
			m = this.testChannel3.receive(0);
			assertThat(m).isNotNull();
			assertThat(new IntegrationMessageHeaderAccessor(m).getPriority()).isEqualTo(Integer.valueOf(9 - i));
		}
		Message<?> m = this.testChannel3.receive(0);
		assertThat(m).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(m).getPriority()).isEqualTo(Integer.valueOf(199));
		assertThat(m.getPayload()).isEqualTo(99);
		m = this.testChannel3.receive(0);
		assertThat(m).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(m).getPriority()).isNull();
		assertThat(m.getPayload()).isEqualTo(98);
		assertThat(this.priorityCms.messageGroupSize("priorityCms:testChannel3")).isZero();

		m = this.testChannel4.receive(0);
		assertThat(m).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(m).getPriority()).isEqualTo(Integer.valueOf(5));
		m = this.testChannel4.receive(0);
		assertThat(m).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(m).getPriority()).isNull();
		assertThat(this.priorityCms.getMessageGroupCount()).isZero();
		assertThat(this.priorityCms.getMessageCountForAllMessageGroups()).isZero();
		assertThat(this.testChannel3.receive(0)).isNull();
		assertThat(this.testChannel4.receive(0)).isNull();

		for (int i = 0; i < 10; i++) {
			this.testChannel3.send(new GenericMessage<>(i));
		}
		assertThat(this.priorityCms.getMessageGroupCount()).isEqualTo(1);
		assertThat(this.priorityCms.messageGroupSize("priorityCms:testChannel3")).isEqualTo(10);
		this.priorityCms.removeMessageGroup("priorityCms:testChannel3");
		assertThat(this.priorityCms.getMessageGroupCount()).isZero();
		assertThat(this.priorityCms.messageGroupSize("priorityCms:testChannel3")).isZero();
	}

	@Test
	void testJsonSerialization() {
		RedisChannelMessageStore store = new RedisChannelMessageStore(RedisContainerTest.connectionFactory());
		ObjectMapper mapper = JacksonJsonUtils.messagingAwareMapper();
		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
		store.setValueSerializer(serializer);

		Message<?> genericMessage = new GenericMessage<>(new Date());
		NullChannel testComponent = new NullChannel();
		testComponent.setBeanName("testChannel");
		genericMessage = MessageHistory.write(genericMessage, testComponent);

		String groupId = "jsonMessagesStore";

		store.addMessageToGroup(groupId, genericMessage);
		MessageGroup messageGroup = store.getMessageGroup(groupId);
		assertThat(messageGroup.size()).isEqualTo(1);
		List<Message<?>> messages = new ArrayList<>(messageGroup.getMessages());
		assertThat(messages.get(0)).isEqualTo(genericMessage);
		assertThat(messages.get(0).getHeaders()).containsKeys(MessageHistory.HEADER_NAME);
	}

}
