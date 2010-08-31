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

package org.springframework.integration.jmx.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.GenericMessage;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicRouterTests {

	@Autowired
	@Qualifier("controlChannel")
	private MessageChannel controlChannel;
	
	@Autowired
	@Qualifier("routingChannel")
	private MessageChannel routingChannel;
	
	@Autowired
	@Qualifier("processAChannel")
	private QueueChannel processAChannel;
	
	@Autowired
	@Qualifier("processBChannel")
	private QueueChannel processBChannel;
	
	@Autowired
	@Qualifier("processCChannel")
	private QueueChannel processCChannel;


	@Test
	public void testRouteChange() throws Exception {
		routingChannel.send(new GenericMessage<String>("123"));
		assertEquals("123", processAChannel.receive().getPayload());
		routingChannel.send(MessageBuilder.withPayload(123).build());
		assertEquals(123, processBChannel.receive().getPayload());

		controlChannel.send(MessageBuilder.withPayload(new String[]{"java.lang.String", "processCChannel"}).build());
		
		routingChannel.send(new GenericMessage<String>("123"));
		assertEquals("123", processCChannel.receive().getPayload());
	}

}
