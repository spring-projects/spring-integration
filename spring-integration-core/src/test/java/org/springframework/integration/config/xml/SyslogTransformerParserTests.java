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
package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.transformer.SyslogToMapTransformer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SyslogTransformerParserTests {

	@Autowired
	private MessageChannel toMapChannel;

	@Autowired
	private PollableChannel out;

	@Test
	public void testMap() {
		toMapChannel.send(new GenericMessage<String>("<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE"));
		Map<?, ?> map = (Map<?, ?>) out.receive(1000).getPayload();
		assertNotNull(map);
		assertEquals(6, map.size());
		assertEquals(19, map.get(SyslogToMapTransformer.FACILITY));
		assertEquals(5, map.get(SyslogToMapTransformer.SEVERITY));
		Object date = map.get(SyslogToMapTransformer.TIMESTAMP);
		assertTrue(date instanceof Date || date instanceof String);
		assertEquals("WEBERN", map.get(SyslogToMapTransformer.HOST));
		assertEquals("TESTING[70729]", map.get(SyslogToMapTransformer.TAG));
		assertEquals("TEST SYSLOG MESSAGE", map.get(SyslogToMapTransformer.MESSAGE));
	}
}
