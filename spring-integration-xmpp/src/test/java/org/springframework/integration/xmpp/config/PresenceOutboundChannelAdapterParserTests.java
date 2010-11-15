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

package org.springframework.integration.xmpp.config;

import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PresenceOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testRosterEventOutboundChannelAdapterParserAsPollingConsumer(){
		Object pollingConsumer = context.getBean("pollingOutboundRosterAdapter");
		assertTrue(pollingConsumer instanceof PollingConsumer);
	}
	
	@Test
	public void testRosterEventOutboundChannelAdapterParserEventConsumer(){
		Object eventConsumer = context.getBean("eventOutboundRosterAdapter");
		assertTrue(eventConsumer instanceof EventDrivenConsumer);
	}
	
	@Test
	public void testRosterEventOutboundChannel(){
		Object channel = context.getBean("eventOutboundRosterChannel");
		assertTrue(channel instanceof SubscribableChannel);
	}

}
