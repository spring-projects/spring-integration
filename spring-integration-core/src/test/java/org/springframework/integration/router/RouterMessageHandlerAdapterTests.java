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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RouterMessageHandlerAdapterTests {

	@Test
	public void testChannelNameResolutionByPayload() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> message = new GenericMessage<String>("123", "bar");
		SimpleChannel barChannel = new SimpleChannel();
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
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		attribs.put("property", "returnAddress");
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> message = new GenericMessage<String>("123", "bar");
		message.getHeader().setProperty("returnAddress", "baz");
		SimpleChannel barChannel = new SimpleChannel();
		SimpleChannel bazChannel = new SimpleChannel();
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
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		attribs.put("attribute", "returnAddress");
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> message = new GenericMessage<String>("123", "bar");
		message.getHeader().setProperty("returnAddress", "bad");
		message.getHeader().setAttribute("returnAddress", "baz");
		SimpleChannel barChannel = new SimpleChannel();
		SimpleChannel badChannel = new SimpleChannel();
		SimpleChannel bazChannel = new SimpleChannel();
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
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		attribs.put("property", "targetChannel");
		attribs.put("attribute", "returnAddress");
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		adapter.afterPropertiesSet();
		adapter.handle(new GenericMessage<String>("123", "testing"));
	}

	@Test
	public void testChannelNameResolutionByMessage() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		SimpleChannel fooChannel = new SimpleChannel();
		SimpleChannel barChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		adapter.handle(barMessage);
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		adapter.handle(badMessage);
		Message<?> result4 = fooChannel.receive(0);
		assertNull(result4);
		Message<?> result5 = barChannel.receive(0);
		assertNull(result5);
	}

	@Test
	public void testChannelInstanceResolutionByPayload() throws Exception {
		SimpleChannel fooChannel = new SimpleChannel();
		SimpleChannel barChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		adapter.handle(barMessage);
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		adapter.handle(badMessage);
		Message<?> result3 = fooChannel.receive(0);
		assertNull(result3);
		Message<?> result4 = barChannel.receive(0);
		assertNull(result4);
	}

	@Test
	public void testChannelInstanceResolutionByMessage() throws Exception {
		SimpleChannel fooChannel = new SimpleChannel();
		SimpleChannel barChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		adapter.handle(barMessage);
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		adapter.handle(badMessage);
		Message<?> result3 = fooChannel.receive(0);
		assertNull(result3);
		Message<?> result4 = barChannel.receive(0);
		assertNull(result4);
	}

	@Test
	public void testMultiChannelNameResolutionByPayload() throws Exception {
		SimpleChannel fooChannel = new SimpleChannel();
		SimpleChannel barChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("foo", result2.getPayload());
		adapter.handle(barMessage);
		Message<?> result3 = fooChannel.receive(0);
		assertNotNull(result3);
		assertEquals("bar", result3.getPayload());
		Message<?> result4 = barChannel.receive(0);
		assertNotNull(result4);
		assertEquals("bar", result4.getPayload());
		adapter.handle(badMessage);
		Message<?> result5 = fooChannel.receive(0);
		assertNull(result5);
		Message<?> result6 = barChannel.receive(0);
		assertNull(result6);
	}

	@Test
	public void testMultiChannelNameResolutionByMessage() throws Exception {
		SimpleChannel fooChannel = new SimpleChannel();
		SimpleChannel barChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("foo", result2.getPayload());
		adapter.handle(barMessage);
		Message<?> result3 = fooChannel.receive(0);
		assertNotNull(result3);
		assertEquals("bar", result3.getPayload());
		Message<?> result4 = barChannel.receive(0);
		assertNotNull(result4);
		assertEquals("bar", result4.getPayload());
		adapter.handle(badMessage);
		Message<?> result5 = fooChannel.receive(0);
		assertNull(result5);
		Message<?> result6 = barChannel.receive(0);
		assertNull(result6);
	}

	@Test
	public void testMultiChannelNameArrayResolutionByMessage() throws Exception {
		SimpleChannel fooChannel = new SimpleChannel();
		SimpleChannel barChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("foo", result2.getPayload());
		adapter.handle(barMessage);
		Message<?> result3 = fooChannel.receive(0);
		assertNotNull(result3);
		assertEquals("bar", result3.getPayload());
		Message<?> result4 = barChannel.receive(0);
		assertNotNull(result4);
		assertEquals("bar", result4.getPayload());
		adapter.handle(badMessage);
		Message<?> result5 = fooChannel.receive(0);
		assertNull(result5);
		Message<?> result6 = barChannel.receive(0);
		assertNull(result6);
	}

	@Test
	public void testMultiChannelListResolutionByPayload() throws Exception {
		SimpleChannel fooChannel = new SimpleChannel();
		SimpleChannel barChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("foo", result2.getPayload());
		adapter.handle(barMessage);
		Message<?> result3 = fooChannel.receive(0);
		assertNotNull(result3);
		assertEquals("bar", result3.getPayload());
		Message<?> result4 = barChannel.receive(0);
		assertNotNull(result4);
		assertEquals("bar", result4.getPayload());
		adapter.handle(badMessage);
		Message<?> result5 = fooChannel.receive(0);
		assertNull(result5);
		Message<?> result6 = barChannel.receive(0);
		assertNull(result6);
	}

	@Test
	public void testMultiChannelListResolutionByMessage() throws Exception {
		SimpleChannel fooChannel = new SimpleChannel();
		SimpleChannel barChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("foo", result2.getPayload());
		adapter.handle(barMessage);
		Message<?> result3 = fooChannel.receive(0);
		assertNotNull(result3);
		assertEquals("bar", result3.getPayload());
		Message<?> result4 = barChannel.receive(0);
		assertNotNull(result4);
		assertEquals("bar", result4.getPayload());
		adapter.handle(badMessage);
		Message<?> result5 = fooChannel.receive(0);
		assertNull(result5);
		Message<?> result6 = barChannel.receive(0);
		assertNull(result6);
	}

	@Test
	public void testMultiChannelArrayResolutionByMessage() throws Exception {
		SimpleChannel fooChannel = new SimpleChannel();
		SimpleChannel barChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod, attribs);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		adapter.handle(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("foo", result2.getPayload());
		adapter.handle(barMessage);
		Message<?> result3 = fooChannel.receive(0);
		assertNotNull(result3);
		assertEquals("bar", result3.getPayload());
		Message<?> result4 = barChannel.receive(0);
		assertNotNull(result4);
		assertEquals("bar", result4.getPayload());
		adapter.handle(badMessage);
		Message<?> result5 = fooChannel.receive(0);
		assertNull(result5);
		Message<?> result6 = barChannel.receive(0);
		assertNull(result6);
	}


	public static class SingleChannelNameRoutingTestBean {

		public String routePayload(String name) {
			return name + "-channel";
		}

		public String routeMessage(Message<?> message) {
			if (message.getPayload().equals("foo")) {
				return "foo-channel";
			}
			else if (message.getPayload().equals("bar")) {
				return "bar-channel";
			}
			return null;
		}
	}


	public static class MultiChannelNameRoutingTestBean {

		public List<String> routePayload(String name) {
			List<String> results = new ArrayList<String>();
			if (name.equals("foo") || name.equals("bar")) {
				results.add("foo-channel");
				results.add("bar-channel");
			}
			return results;
		}

		public List<String> routeMessage(Message<?> message) {
			List<String> results = new ArrayList<String>();
			if (message.getPayload().equals("foo") || message.getPayload().equals("bar")) {
				results.add("foo-channel");
				results.add("bar-channel");
			}
			return results;
		}

		public String[] routeMessageToArray(Message<?> message) {
			String[] results = null;
			if (message.getPayload().equals("foo") || message.getPayload().equals("bar")) {
				results = new String[2];
				results[0] = "foo-channel";
				results[1] = "bar-channel";
			}
			return results;
		}
	}


	public static class SingleChannelInstanceRoutingTestBean {

		private ChannelRegistry registry;

		public SingleChannelInstanceRoutingTestBean(ChannelRegistry registry) {
			this.registry = registry;
		}

		public MessageChannel routePayload(String name) {
			return registry.lookupChannel(name + "-channel");
		}

		public MessageChannel routeMessage(Message<?> message) {
			if (message.getPayload().equals("foo")) {
				return registry.lookupChannel("foo-channel");
			}
			else if (message.getPayload().equals("bar")) {
				return registry.lookupChannel("bar-channel");
			}
			return null;
		}
	}


	public static class MultiChannelInstanceRoutingTestBean {

		private ChannelRegistry registry;

		public MultiChannelInstanceRoutingTestBean(ChannelRegistry registry) {
			this.registry = registry;
		}

		public List<MessageChannel> routePayload(String name) {
			List<MessageChannel> results = new ArrayList<MessageChannel>();
			if (name.equals("foo") || name.equals("bar")) {
				results.add(registry.lookupChannel("foo-channel"));
				results.add(registry.lookupChannel("bar-channel"));
			}
			return results;
		}

		public List<MessageChannel> routeMessage(Message<?> message) {
			List<MessageChannel> results = new ArrayList<MessageChannel>();
			if (message.getPayload().equals("foo") || message.getPayload().equals("bar")) {
				results.add(registry.lookupChannel("foo-channel"));
				results.add(registry.lookupChannel("bar-channel"));
			}
			return results;
		}

		public MessageChannel[] routeMessageToArray(Message<?> message) {
			MessageChannel[] results = null;
			if (message.getPayload().equals("foo") || message.getPayload().equals("bar")) {
				results = new MessageChannel[2];
				results[0] = registry.lookupChannel("foo-channel");
				results[1] = registry.lookupChannel("bar-channel");
			}
			return results;
		}
	}

}
