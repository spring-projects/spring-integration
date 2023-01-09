/*
 * Copyright 2014-2023 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class RedisChannelMessageStoreTests extends RedisAvailableTests {

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

	@Before
	@After
	public void setUpTearDown() {
		this.cms.removeMessageGroup("cms:testChannel1");
		this.cms.removeMessageGroup("cms:testChannel2");
		this.priorityCms.removeMessageGroup("priorityCms:testChannel3");
		this.priorityCms.removeMessageGroup("priorityCms:testChannel4");
	}

	@Test
	@RedisAvailable
	public void testChannel() {
		for (int i = 0; i < 10; i++) {
			this.testChannel1.send(new GenericMessage<Integer>(i));
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
		assertThat(this.cms.getMessageGroupCount()).isEqualTo(0);

		for (int i = 0; i < 10; i++) {
			this.testChannel1.send(new GenericMessage<Integer>(i));
		}
		assertThat(this.cms.getMessageGroupCount()).isEqualTo(1);
		assertThat(this.cms.messageGroupSize("cms:testChannel1")).isEqualTo(10);
		this.cms.removeMessageGroup("cms:testChannel1");
		assertThat(this.cms.getMessageGroupCount()).isEqualTo(0);
		assertThat(this.cms.messageGroupSize("cms:testChannel1")).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void testPriority() {
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
		assertThat(this.priorityCms.messageGroupSize("priorityCms:testChannel3")).isEqualTo(0);

		m = this.testChannel4.receive(0);
		assertThat(m).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(m).getPriority()).isEqualTo(Integer.valueOf(5));
		m = this.testChannel4.receive(0);
		assertThat(m).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(m).getPriority()).isNull();
		assertThat(this.priorityCms.getMessageGroupCount()).isEqualTo(0);
		assertThat(this.priorityCms.getMessageCountForAllMessageGroups()).isEqualTo(0);
		assertThat(this.testChannel3.receive(0)).isNull();
		assertThat(this.testChannel4.receive(0)).isNull();

		for (int i = 0; i < 10; i++) {
			this.testChannel3.send(new GenericMessage<Integer>(i));
		}
		assertThat(this.priorityCms.getMessageGroupCount()).isEqualTo(1);
		assertThat(this.priorityCms.messageGroupSize("priorityCms:testChannel3")).isEqualTo(10);
		this.priorityCms.removeMessageGroup("priorityCms:testChannel3");
		assertThat(this.priorityCms.getMessageGroupCount()).isEqualTo(0);
		assertThat(this.priorityCms.messageGroupSize("priorityCms:testChannel3")).isEqualTo(0);
	}

}
