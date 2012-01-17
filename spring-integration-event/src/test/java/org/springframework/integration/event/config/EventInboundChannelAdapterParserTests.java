/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.event.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EventInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	MessageChannel errorChannel;

	@Autowired
	MessageChannel autoChannel;

	@Autowired @Qualifier("autoChannel.adapter")
	ApplicationEventListeningMessageProducer eventListener;

	@Test
	public void validateEventParser() {
		Object adapter = context.getBean("eventAdapterSimple");
		Assert.assertNotNull(adapter);
		Assert.assertTrue(adapter instanceof ApplicationEventListeningMessageProducer);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Assert.assertEquals(context.getBean("input"), adapterAccessor.getPropertyValue("outputChannel"));
		Assert.assertSame(errorChannel, adapterAccessor.getPropertyValue("errorChannel"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void validateEventParserWithEventTypes() {
		Object adapter = context.getBean("eventAdapterFiltered");
		Assert.assertNotNull(adapter);
		Assert.assertTrue(adapter instanceof ApplicationEventListeningMessageProducer);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Assert.assertEquals(context.getBean("inputFiltered"), adapterAccessor.getPropertyValue("outputChannel"));
		Set<Class<? extends ApplicationEvent>> eventTypes = (Set<Class<? extends ApplicationEvent>>) adapterAccessor.getPropertyValue("eventTypes");
		assertNotNull(eventTypes);
		assertTrue(eventTypes.size() == 2);	
		assertTrue(eventTypes.contains(SampleEvent.class));
		assertTrue(eventTypes.contains(AnotherSampleEvent.class));
		assertNull(adapterAccessor.getPropertyValue("errorChannel"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void validateEventParserWithEventTypesAndPlaceholder() {
		Object adapter = context.getBean("eventAdapterFilteredPlaceHolder");
		Assert.assertNotNull(adapter);
		Assert.assertTrue(adapter instanceof ApplicationEventListeningMessageProducer);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Assert.assertEquals(context.getBean("inputFilteredPlaceHolder"), adapterAccessor.getPropertyValue("outputChannel"));
		Set<Class<? extends ApplicationEvent>> eventTypes = (Set<Class<? extends ApplicationEvent>>) adapterAccessor.getPropertyValue("eventTypes");
		assertNotNull(eventTypes);
		assertTrue(eventTypes.size() == 2);	
		assertTrue(eventTypes.contains(SampleEvent.class));
		assertTrue(eventTypes.contains(AnotherSampleEvent.class));
	}

	@Test
	public void validateUsageWithHistory() {
		PollableChannel channel = context.getBean("input", PollableChannel.class);
		assertEquals(ContextRefreshedEvent.class, channel.receive(0).getPayload().getClass());
		context.publishEvent(new SampleEvent("hello"));
		Message<?> message = channel.receive(0);
		MessageHistory history = MessageHistory.read(message);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "eventAdapterSimple", 0);
		assertNotNull(componentHistoryRecord);
		assertEquals("event:inbound-channel-adapter", componentHistoryRecord.get("type"));
		assertNotNull(message);
		assertEquals(SampleEvent.class, message.getPayload().getClass());
	}

	@Test
	public void validatePayloadExpression() {
		Object adapter = context.getBean("eventAdapterSpel");
		Assert.assertNotNull(adapter);
		Assert.assertTrue(adapter instanceof ApplicationEventListeningMessageProducer);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Expression expression = (Expression) adapterAccessor.getPropertyValue("payloadExpression");
		Assert.assertEquals("source + '-test'", expression.getExpressionString());
	}

	@Test
	public void testAutoCreateChannel() {
		assertSame(autoChannel, TestUtils.getPropertyValue(eventListener, "outputChannel"));
	}

	@SuppressWarnings("serial")
	public static class SampleEvent extends ApplicationEvent {
		public SampleEvent(Object source) {
			super(source);
		}
	}

	@SuppressWarnings("serial")
	public static class AnotherSampleEvent extends ApplicationEvent {
		public AnotherSampleEvent(Object source) {
			super(source);
		}
	}
}
