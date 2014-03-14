/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.redis.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.message.MutableMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
	public void setup() {
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
		assertEquals(1, this.cms.getMessageGroupCount());
		assertEquals(10, this.cms.messageGroupSize("cms:testChannel1"));
		assertEquals(10, this.priorityCms.getMessageGroup("cms:testChannel1").size());
		for (int i = 0; i < 10; i++) {
			this.testChannel2.send(new MutableMessage<Integer>(i));
		}
		assertEquals(2, this.cms.getMessageGroupCount());
		assertEquals(10, this.cms.messageGroupSize("cms:testChannel2"));
		assertEquals(10, this.priorityCms.getMessageGroup("cms:testChannel2").size());
		assertEquals(20, this.cms.getMessageCountForAllMessageGroups());
		for (int i = 0; i < 10; i++) {
			Message<?> out = this.testChannel1.receive(0);
			assertThat(out, Matchers.instanceOf(GenericMessage.class));
			assertEquals(Integer.valueOf(i), out.getPayload());
		}
		assertNull(this.testChannel1.receive(0));
		for (int i = 0; i < 10; i++) {
			Message<?> out = this.testChannel2.receive(0);
			assertThat(out, Matchers.instanceOf(MutableMessage.class));
			assertEquals(Integer.valueOf(i), out.getPayload());
		}
		assertNull(this.testChannel2.receive(0));
		assertEquals(0, this.cms.getMessageGroupCount());

		for (int i = 0; i < 10; i++) {
			this.testChannel1.send(new GenericMessage<Integer>(i));
		}
		assertEquals(1, this.cms.getMessageGroupCount());
		assertEquals(10, this.cms.messageGroupSize("cms:testChannel1"));
		this.cms.removeMessageGroup("cms:testChannel1");
		assertEquals(0, this.cms.getMessageGroupCount());
		assertEquals(0, this.cms.messageGroupSize("cms:testChannel1"));
	}

	@Test
	public void testPriority() {
		for (int i = 0; i < 10; i++) {
			Message<Integer> message = MessageBuilder.withPayload(i).setPriority(9-i).build();
			this.testChannel3.send(message);
			this.testChannel3.send(message);
		}
		this.testChannel3.send(MessageBuilder.withPayload(99).setPriority(199).build());
		this.testChannel3.send(MessageBuilder.withPayload(98).build());
		assertEquals(1, this.priorityCms.getMessageGroupCount());
		assertEquals(22, this.priorityCms.messageGroupSize("priorityCms:testChannel3"));
		assertEquals(22, this.priorityCms.getMessageCountForAllMessageGroups());
		assertEquals(22, this.priorityCms.getMessageGroup("priorityCms:testChannel3").size());
		this.testChannel4.send(MessageBuilder.withPayload(98).build());
		this.testChannel4.send(MessageBuilder.withPayload(99).setPriority(5).build());
		assertEquals(2, this.priorityCms.getMessageGroupCount());
		assertEquals(2, this.priorityCms.getMessageGroup("priorityCms:testChannel4").size());
		assertEquals(2, this.priorityCms.messageGroupSize("priorityCms:testChannel4"));
		assertEquals(24, this.priorityCms.getMessageCountForAllMessageGroups());
		for (int i = 0; i < 10; i++) {
			Message<?> m = this.testChannel3.receive(0);
			assertNotNull(m);
			assertEquals(Integer.valueOf(i), new IntegrationMessageHeaderAccessor(m).getPriority());
			assertEquals(Integer.valueOf(9-i), m.getPayload());
			m = this.testChannel3.receive(0);
			assertNotNull(m);
			assertEquals(Integer.valueOf(i), new IntegrationMessageHeaderAccessor(m).getPriority());
		}
		Message<?> m = this.testChannel3.receive(0);
		assertNotNull(m);
		assertEquals(Integer.valueOf(199), new IntegrationMessageHeaderAccessor(m).getPriority());
		assertEquals(Integer.valueOf(99), m.getPayload());
		m = this.testChannel3.receive(0);
		assertNotNull(m);
		assertNull(new IntegrationMessageHeaderAccessor(m).getPriority());
		assertEquals(Integer.valueOf(98), m.getPayload());
		assertEquals(0, this.priorityCms.messageGroupSize("priorityCms:testChannel3"));

		m = this.testChannel4.receive(0);
		assertNotNull(m);
		assertEquals(Integer.valueOf(5), new IntegrationMessageHeaderAccessor(m).getPriority());
		m = this.testChannel4.receive(0);
		assertNotNull(m);
		assertNull(new IntegrationMessageHeaderAccessor(m).getPriority());
		assertEquals(0, this.priorityCms.getMessageGroupCount());
		assertEquals(0, this.priorityCms.getMessageCountForAllMessageGroups());
		assertNull(this.testChannel3.receive(0));
		assertNull(this.testChannel4.receive(0));

		for (int i = 0; i < 10; i++) {
			this.testChannel3.send(new GenericMessage<Integer>(i));
		}
		assertEquals(1, this.priorityCms.getMessageGroupCount());
		assertEquals(10, this.priorityCms.messageGroupSize("priorityCms:testChannel3"));
		this.priorityCms.removeMessageGroup("priorityCms:testChannel3");
		assertEquals(0, this.priorityCms.getMessageGroupCount());
		assertEquals(0, this.priorityCms.messageGroupSize("priorityCms:testChannel3"));
	}

}
