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

package org.springframework.integration.channel.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.SubscribableSource;

/**
 * @author Mark Fisher
 */
public class ChannelAdapterFactoryBeanTests {

	@Test
	public void testPollableSourceWithoutTarget() throws Exception {
		ChannelAdapterFactoryBean factoryBean = new ChannelAdapterFactoryBean();
		factoryBean.setBeanName("testChannel");
		factoryBean.setSource(new TestPollableSource());
		Object bean = factoryBean.getObject();
		assertTrue(bean instanceof PollableChannel);
		PollableChannel channel = (PollableChannel) bean;
		Message<?> reply = channel.receive();
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
		assertFalse(channel.send(new StringMessage("no target")));
	}

	@Test
	public void testSubscribableSourceWithoutTarget() throws Exception {
		ChannelAdapterFactoryBean factoryBean = new ChannelAdapterFactoryBean();
		factoryBean.setBeanName("testChannel");
		TestSubscribableSource source = new TestSubscribableSource();
		factoryBean.setSource(source);
		Object bean = factoryBean.getObject();
		assertTrue(bean instanceof MessageChannel);
		assertTrue(bean instanceof SubscribableSource);
		MessageChannel channel = (MessageChannel) bean;
		final AtomicReference<Message<?>> messageReference = new AtomicReference<Message<?>>();
		((SubscribableSource) bean).subscribe(new MessageTarget() {
			public boolean send(Message<?> message) {
				messageReference.set(message);
				return true;
			}
		});
		source.publishMessage(new StringMessage("foo"));
		assertNotNull(messageReference.get());
		assertEquals("foo", (messageReference.get()).getPayload());
		assertFalse(channel.send(new StringMessage("no target")));
	}

	@Test
	public void testTargetOnly() throws Exception {
		ChannelAdapterFactoryBean factoryBean = new ChannelAdapterFactoryBean();
		factoryBean.setBeanName("testChannel");
		final AtomicReference<Message<?>> messageRef = new AtomicReference<Message<?>>();
		factoryBean.setTarget(new MessageTarget() {
			public boolean send(Message<?> message) {
				messageRef.set(message);
				return true;
			}
		});
		Object bean = factoryBean.getObject();
		assertTrue(bean instanceof PollableChannel);
		PollableChannel channel = (PollableChannel) bean;
		Message<?> reply = channel.receive(0);
		assertNull(reply);
		assertTrue(channel.send(new StringMessage("hello")));
		assertNotNull(messageRef.get());
		assertEquals("hello", messageRef.get().getPayload());
	}

}
