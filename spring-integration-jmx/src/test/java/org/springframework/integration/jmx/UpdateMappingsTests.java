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
package org.springframework.integration.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class UpdateMappingsTests {

	@Autowired
	private MessageChannel control;

	@Autowired
	private MessageChannel in;

	@Autowired
	private PollableChannel qux;

	@Autowired
	private MBeanServer server;

	@Test
	public void test() {
		control.send(new GenericMessage<String>("@myRouter.setChannelMapping('baz', 'qux')"));
		Message<?> message = MessageBuilder.withPayload("Hello, world!")
				.setHeader("routing.header", "baz").build();
		in.send(message);
		assertNotNull(qux.receive());
	}

	@Test
	public void testChangeRouterMappings() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(control,
				"@'router.handler'.replaceChannelMappings('foo=bar \n baz=qux')");
		Map<?, ?> mappings = messagingTemplate.convertSendAndReceive(control, "@'router.handler'.getChannelMappings()", Map.class);
		assertNotNull(mappings);
		assertEquals(2, mappings.size());
		assertEquals("bar", mappings.get("foo"));
		assertEquals("qux", mappings.get("baz"));
		messagingTemplate.convertAndSend(control,
				"@'router.handler'.replaceChannelMappings('foo=qux \n baz=bar')");
		mappings = messagingTemplate.convertSendAndReceive(control, "@'router.handler'.getChannelMappings()", Map.class);
		assertEquals(2, mappings.size());
		assertEquals("bar", mappings.get("baz"));
		assertEquals("qux", mappings.get("foo"));
	}

	@Test
	public void testJmx() throws Exception {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		Set<ObjectName> names = this.server.queryNames(ObjectName
				.getInstance("update.mapping.domain:type=HeaderValueRouter,name=router"),
				null);
		assertEquals(1, names.size());
		Map<String, String> map = new HashMap<String, String>();
		map.put("foo", "bar");
		map.put("baz", "qux");
		Object[] params = new Object[] {map};
		this.server.invoke(names.iterator().next(), "setChannelMappings", params,
				new String[] { "java.util.Map" });
		Map<?, ?> mappings = messagingTemplate.convertSendAndReceive(control, "@'router.handler'.getChannelMappings()", Map.class);
		assertNotNull(mappings);
		assertEquals(2, mappings.size());
		assertEquals("bar", mappings.get("foo"));
		assertEquals("qux", mappings.get("baz"));
	}

}
