/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.stomp.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.stomp.StompSessionManager;
import org.springframework.integration.stomp.inbound.StompInboundChannelAdapter;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;

/**
 * @author Artem Bilan
 * @since 4.2
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class StompAdaptersParserTests {

	@Autowired
	private StompSessionManager stompSessionManager;

	@Autowired
	private HeaderMapper<?> headerMapper;

	@Autowired
	@Qualifier("defaultInboundAdapter")
	private MessageChannel defaultInboundAdapterChannel;

	@Autowired
	private MessageChannel errorChannel;

	@Autowired
	private MessageChannel inboundChannel;
	@Autowired
	@Qualifier("defaultInboundAdapter.adapter")
	private StompInboundChannelAdapter defaultInboundAdapter;

	@Autowired
	private StompInboundChannelAdapter customInboundAdapter;

	@Autowired
	@Qualifier("defaultOutboundAdapter")
	private MessageChannel defaultOutboundAdapterChannel;

	@Autowired
	@Qualifier("defaultOutboundAdapter.handler")
	private MessageHandler defaultOutboundAdapterHandler;

	@Autowired
	@Qualifier("defaultOutboundAdapter.adapter")
	private AbstractEndpoint defaultOutboundAdapter;

	@Autowired
	private MessageChannel outboundChannel;

	@Autowired
	@Qualifier("customOutboundAdapter.handler")
	private MessageHandler customOutboundAdapterHandler;

	@Autowired
	@Qualifier("customOutboundAdapter")
	private AbstractEndpoint customOutboundAdapter;

	@Autowired
	private SmartLifecycleRoleController roleController;

	@Test
	public void testParsers() {
		assertSame(this.defaultInboundAdapterChannel,
				TestUtils.getPropertyValue(this.defaultInboundAdapter, "outputChannel"));
		assertSame(this.stompSessionManager,
				TestUtils.getPropertyValue(this.defaultInboundAdapter, "stompSessionManager"));
		assertNull(TestUtils.getPropertyValue(this.defaultInboundAdapter, "errorChannel"));
		Object headerMapper = TestUtils.getPropertyValue(this.defaultInboundAdapter, "headerMapper");
		assertNotNull(headerMapper);
		assertNotSame(this.headerMapper, headerMapper);
		assertEquals(String.class, TestUtils.getPropertyValue(this.defaultInboundAdapter, "payloadType", Class.class));
		assertTrue(TestUtils.getPropertyValue(this.defaultInboundAdapter, "autoStartup", Boolean.class));

		assertSame(this.inboundChannel,
				TestUtils.getPropertyValue(this.customInboundAdapter, "outputChannel"));
		assertSame(this.stompSessionManager,
				TestUtils.getPropertyValue(this.customInboundAdapter, "stompSessionManager"));
		assertSame(this.errorChannel, TestUtils.getPropertyValue(this.customInboundAdapter, "errorChannel"));
		assertEquals(Collections.singleton("foo"),
				TestUtils.getPropertyValue(this.customInboundAdapter, "destinations"));
		headerMapper = TestUtils.getPropertyValue(this.customInboundAdapter, "headerMapper");
		assertNotNull(headerMapper);
		assertNotSame(this.headerMapper, headerMapper);
		assertArrayEquals(new String[] {"bar", "foo"},
				TestUtils.getPropertyValue(headerMapper, "inboundHeaderNames", String[].class));
		assertEquals(Integer.class, TestUtils.getPropertyValue(this.customInboundAdapter, "payloadType", Class.class));
		assertFalse(TestUtils.getPropertyValue(this.customInboundAdapter, "autoStartup", Boolean.class));
		assertEquals(200, TestUtils.getPropertyValue(this.customInboundAdapter, "phase"));
		assertEquals(2000L, TestUtils.getPropertyValue(this.customInboundAdapter, "messagingTemplate.sendTimeout"));

		assertSame(this.stompSessionManager,
				TestUtils.getPropertyValue(this.defaultOutboundAdapterHandler, "stompSessionManager"));
		headerMapper = TestUtils.getPropertyValue(this.defaultOutboundAdapterHandler, "headerMapper");
		assertNotNull(headerMapper);
		assertNotSame(this.headerMapper, headerMapper);
		assertNull(TestUtils.getPropertyValue(this.defaultOutboundAdapterHandler, "destinationExpression"));
		assertSame(this.defaultOutboundAdapterHandler,
				TestUtils.getPropertyValue(this.defaultOutboundAdapter, "handler"));
		assertSame(this.defaultOutboundAdapterChannel,
				TestUtils.getPropertyValue(this.defaultOutboundAdapter, "inputChannel"));
		assertTrue(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "autoStartup", Boolean.class));

		assertSame(this.stompSessionManager,
				TestUtils.getPropertyValue(this.customOutboundAdapterHandler, "stompSessionManager"));
		assertSame(this.headerMapper, TestUtils.getPropertyValue(this.customOutboundAdapterHandler, "headerMapper"));
		assertEquals("baz",
				TestUtils.getPropertyValue(this.customOutboundAdapterHandler, "destinationExpression.literalValue"));
		assertSame(this.customOutboundAdapterHandler,
				TestUtils.getPropertyValue(this.customOutboundAdapter, "handler"));
		assertSame(this.outboundChannel, TestUtils.getPropertyValue(this.customOutboundAdapter, "inputChannel"));
		assertFalse(TestUtils.getPropertyValue(this.customOutboundAdapter, "autoStartup", Boolean.class));
		assertEquals(100, TestUtils.getPropertyValue(this.customOutboundAdapter, "phase"));

		@SuppressWarnings("unchecked")
		MultiValueMap<String, SmartLifecycle> lifecycles = (MultiValueMap<String, SmartLifecycle>)
				TestUtils.getPropertyValue(this.roleController, "lifecycles", MultiValueMap.class);
		assertTrue(lifecycles.containsKey("bar"));
		List<SmartLifecycle> bars = lifecycles.get("bar");
		bars.contains(this.customInboundAdapter);
		assertTrue(lifecycles.containsKey("foo"));
		bars.contains(this.customOutboundAdapter);
	}

}
