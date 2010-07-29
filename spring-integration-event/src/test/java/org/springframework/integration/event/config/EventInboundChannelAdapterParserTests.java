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

package org.springframework.integration.event.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.event.ApplicationEventInboundChannelAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EventInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void validateEventParser() {
		Object adapter = context.getBean("eventAdapter");
		Assert.assertNotNull(adapter);
		Assert.assertTrue(adapter instanceof ApplicationEventInboundChannelAdapter);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Assert.assertEquals(context.getBean("input"), adapterAccessor.getPropertyValue("outputChannel"));
	}

	@Test
	public void validateUsage() {
		PollableChannel channel = context.getBean("input", PollableChannel.class);
		assertEquals(ContextRefreshedEvent.class, channel.receive(0).getPayload().getClass());
		context.publishEvent(new SampleEvent("hello"));
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals(SampleEvent.class, message.getPayload().getClass());
	}


	@SuppressWarnings("serial")
	public static class SampleEvent extends ApplicationEvent {

		public SampleEvent(Object source) {
			super(source);
		}
		
	}

}
