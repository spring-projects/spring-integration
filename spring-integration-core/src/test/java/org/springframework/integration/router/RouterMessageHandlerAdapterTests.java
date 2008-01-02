/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class RouterMessageHandlerAdapterTests {

	@Test
	public void testChannelNameResolutionByPayload() throws Exception {
		RoutingTestBean testBean = new RoutingTestBean();
		Method fooMethod = testBean.getClass().getMethod("foo", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, fooMethod, attribs);
		Message<String> message = new GenericMessage<String>("123", "bar");
		PointToPointChannel barChannel = new PointToPointChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("bar-channel", barChannel);
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(message);
		Message<?> result = barChannel.receive(0);
		assertNotNull(result);
		assertEquals("bar", result.getPayload());
	}

	@Test
	public void testChannelNameResolutionByProperty() throws Exception {
		RoutingTestBean testBean = new RoutingTestBean();
		Method fooMethod = testBean.getClass().getMethod("foo", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		attribs.put("property", "returnAddress");
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, fooMethod, attribs);
		Message<String> message = new GenericMessage<String>("123", "bar");
		message.getHeader().setProperty("returnAddress", "baz");
		PointToPointChannel barChannel = new PointToPointChannel();
		PointToPointChannel bazChannel = new PointToPointChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("bar-channel", barChannel);
		channelRegistry.registerChannel("baz-channel", bazChannel);
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(message);
		Message<?> message1 = barChannel.receive(0);
		assertNull(message1);
		Message<?> message2 = bazChannel.receive(0);
		assertNotNull(message2);
		assertEquals("bar", message2.getPayload());
	}

	@Test
	public void testChannelNameResolutionByAttribute() throws Exception {
		RoutingTestBean testBean = new RoutingTestBean();
		Method fooMethod = testBean.getClass().getMethod("foo", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		attribs.put("attribute", "returnAddress");
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, fooMethod, attribs);
		Message<String> message = new GenericMessage<String>("123", "bar");
		message.getHeader().setProperty("returnAddress", "bad");
		message.getHeader().setAttribute("returnAddress", "baz");
		PointToPointChannel barChannel = new PointToPointChannel();
		PointToPointChannel badChannel = new PointToPointChannel();
		PointToPointChannel bazChannel = new PointToPointChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("bar-channel", barChannel);
		channelRegistry.registerChannel("bad-channel", badChannel);
		channelRegistry.registerChannel("baz-channel", bazChannel);
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(message);
		Message<?> message1 = barChannel.receive(0);
		assertNull(message1);
		Message<?> message2 = badChannel.receive(0);
		assertNull(message2);
		Message<?> message3 = bazChannel.receive(0);
		assertNotNull(message3);
		assertEquals("bar", message3.getPayload());
	}

	@Test(expected=MessagingConfigurationException.class)
	public void testFailsWhenPropertyAndAttributeAreBothProvided() throws Exception {
		RoutingTestBean testBean = new RoutingTestBean();
		Method fooMethod = testBean.getClass().getMethod("foo", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		attribs.put("property", "targetChannel");
		attribs.put("attribute", "returnAddress");
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, fooMethod, attribs);
		adapter.afterPropertiesSet();
		adapter.handle(new GenericMessage<String>("123", "testing"));
	}


	public static class RoutingTestBean {

		public String foo(String name) {
			return name + "-channel";
		}
	}

}
