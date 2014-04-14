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
package org.springframework.integration.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class JmxRouterControlBusTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Test
	public void testRouterMappings() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'router.handler'.getChannelMappings()");
		Message<?> result = this.output.receive(0);
		assertNotNull(result);
		Map<?, ?> mappings = (Map<?, ?>) result.getPayload();
		assertEquals("bar", mappings.get("foo"));
		assertEquals("qux", mappings.get("baz"));
		messagingTemplate.convertAndSend(input,
				"@'router.handler'.replaceChannelMappings('foo=qux \n baz=bar')");
		messagingTemplate.convertAndSend(input, "@'router.handler'.getChannelMappings()");
		result = this.output.receive(0);
		assertNotNull(result);
		mappings = (Map<?, ?>) result.getPayload();
		assertEquals("bar", mappings.get("baz"));
		assertEquals("qux", mappings.get("foo"));
	}

}
