/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.router.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SpelRouterIntegrationTests {

	@Autowired
	private MessageChannel simpleInput;

	@Autowired
	private PollableChannel even;

	@Autowired
	private PollableChannel odd;

	@Autowired
	private MessageChannel beanResolvingInput;

	@Autowired
	private TestBean testBean;


	@Test
	public void simpleExpressionBasedRouter() {
		TestBean testBean1 = new TestBean(1);
		TestBean testBean2 = new TestBean(2);
		TestBean testBean3 = new TestBean(3);
		TestBean testBean4 = new TestBean(4);
		Message<?> message1 = MessageBuilder.withPayload(testBean1).build();
		Message<?> message2 = MessageBuilder.withPayload(testBean2).build();
		Message<?> message3 = MessageBuilder.withPayload(testBean3).build();
		Message<?> message4 = MessageBuilder.withPayload(testBean4).build();
		this.simpleInput.send(message1);
		this.simpleInput.send(message2);
		this.simpleInput.send(message3);
		this.simpleInput.send(message4);
		assertEquals(testBean1, odd.receive(0).getPayload());
		assertEquals(testBean2, even.receive(0).getPayload());
		assertEquals(testBean3, odd.receive(0).getPayload());
		assertEquals(testBean4, even.receive(0).getPayload());
		assertNull(odd.receive(0));
		assertNull(even.receive(0));
	}

	@Test
	public void beanResolvingExpressionBasedRouter() {
		this.beanResolvingInput.send(MessageBuilder.withPayload(5).build());
		this.beanResolvingInput.send(MessageBuilder.withPayload(9).build());
		this.beanResolvingInput.send(MessageBuilder.withPayload(20).build());
		this.beanResolvingInput.send(MessageBuilder.withPayload(30).build());
		this.beanResolvingInput.send(MessageBuilder.withPayload(34).build());
		assertEquals(20, testBean.clear.receive(0).getPayload());
		assertEquals(30, testBean.clear.receive(0).getPayload());
		assertNull(testBean.clear.receive(0));
		assertEquals(5, testBean.remainders.receive(0).getPayload());
		assertEquals(9, testBean.remainders.receive(0).getPayload());
		assertEquals(34, testBean.remainders.receive(0).getPayload());
		assertNull(testBean.remainders.receive(0));
	}


	static class TestBean {

		private final int number;

		private final QueueChannel clear = new QueueChannel();

		private final QueueChannel remainders = new QueueChannel();

		public TestBean(int number) {
			this.number = number;
		}

		public int getNumber() {
			return this.number;
		}

		public MessageChannel getChannel(int value) {
			return (value == 0) ? clear : remainders;
		}
	}

}
