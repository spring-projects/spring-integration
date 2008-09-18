/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.endpoint.OutboundChannelAdapter;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
public class ChannelAdapterParserTests extends AbstractJUnit4SpringContextTests {

	@Test
	public void targetOnly() {
		String beanName = "outboundWithImplicitChannel";
		Object channel = this.applicationContext.getBean(beanName);
		assertTrue(channel instanceof DirectChannel);
		MessageBus bus = (MessageBus) this.applicationContext.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		bus.start();
		assertNotNull(bus.lookupChannel(beanName));
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertNotNull(adapter);
		assertTrue(adapter instanceof OutboundChannelAdapter);
		TestTarget target = (TestTarget) this.applicationContext.getBean("target");
		assertNull(target.getLastMessage());
		Message<?> message = new StringMessage("test");
		assertTrue(((MessageChannel) channel).send(message));
		assertNotNull(target.getLastMessage());
		assertEquals(message, target.getLastMessage());
		bus.stop();
	}

	@Test
	public void methodInvokingTarget() {
		String beanName = "methodInvokingTarget";
		Object channel = this.applicationContext.getBean(beanName);
		assertTrue(channel instanceof DirectChannel);
		MessageBus bus = (MessageBus) this.applicationContext.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		bus.start();
		assertNotNull(bus.lookupChannel(beanName));
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertNotNull(adapter);
		assertTrue(adapter instanceof OutboundChannelAdapter);
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		assertNull(testBean.getMessage());
		Message<?> message = new StringMessage("target test");
		assertTrue(((MessageChannel) channel).send(message));
		assertNotNull(testBean.getMessage());
		assertEquals("target test", testBean.getMessage());
		bus.stop();
	}

	@Test
	public void methodInvokingSource() {
		String beanName = "methodInvokingSource";
		PollableChannel channel =  (PollableChannel) this.applicationContext.getBean("queueChannel");
		MessageBus bus = (MessageBus) this.applicationContext.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		assertNull(bus.lookupChannel(beanName));
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		bus.start();
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		bus.stop();
	}

}
