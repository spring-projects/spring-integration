/*
 * Copyright 2002-2009 the original author or authors.
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
/**
 * Parser for the &lt;recipient-list-router/&gt; element.
 * 
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RecipientListRouterParserTests {
	@Autowired
	private ConfigurableApplicationContext context;
	@Autowired
	@Qualifier("routingChannel")
	private MessageChannel channel;

	@Test
	public void testRecipientListRouterNamespaceConfig() {
		context.start();
		
		Message message = new GenericMessage<Integer>(1);
		channel.send(message);
		
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		assertTrue(chanel1.receive().getPayload().equals(1));
		assertTrue(chanel2.receive().getPayload().equals(1));
		
	}
	

}
