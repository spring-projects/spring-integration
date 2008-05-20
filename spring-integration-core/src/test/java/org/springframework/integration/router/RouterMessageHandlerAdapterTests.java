/*
 * Copyright 2002-2008 the original author or authors.
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

import org.junit.Test;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.annotation.HeaderAttribute;
import org.springframework.integration.handler.annotation.HeaderProperty;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RouterMessageHandlerAdapterTests {

	@Test
	public void testChannelNameResolutionByPayloadConfiguredByMethodReference() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		this.doTestChannelNameResolutionByPayload(adapter);
	}

	@Test
	public void testChannelNameResolutionByPayloadConfiguredByMethodName() {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, "routePayload");
		this.doTestChannelNameResolutionByPayload(adapter);
	}

	private void doTestChannelNameResolutionByPayload(RouterMessageHandlerAdapter adapter) {
		Message<String> message = new GenericMessage<String>("123", "bar");
		QueueChannel barChannel = new QueueChannel();
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
		Method routingMethod = testBean.getClass().getMethod("routeByProperty", String.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		Message<String> message = new GenericMessage<String>("123", "bar");
		message.getHeader().setProperty("returnAddress", "baz");
		QueueChannel barChannel = new QueueChannel();
		QueueChannel bazChannel = new QueueChannel();
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
		Method routingMethod = testBean.getClass().getMethod("routeByAttribute", String.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		Message<String> message = new GenericMessage<String>("123", "bar");
		message.getHeader().setProperty("returnAddress", "bad");
		message.getHeader().setAttribute("returnAddress", "baz");
		QueueChannel barChannel = new QueueChannel();
		QueueChannel badChannel = new QueueChannel();
		QueueChannel bazChannel = new QueueChannel();
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

	@Test(expected=ConfigurationException.class)
	public void testFailsWhenPropertyAndAttributeAreBothProvided() throws Exception {
		InvalidRoutingTestBean testBean = new InvalidRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("tooManyAnnotations", String.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		adapter.afterPropertiesSet();
		adapter.handle(new GenericMessage<String>("123", "testing"));
	}

	@Test
	public void testChannelNameResolutionByMessageConfiguredByMethodReference() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		this.doTestChannelNameResolutionByMessage(adapter);
	}

	@Test
	public void testChannelNameResolutionByMessageConfiguredByMethodName() {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, "routeMessage");
		this.doTestChannelNameResolutionByMessage(adapter);
	}

	private void doTestChannelNameResolutionByMessage(RouterMessageHandlerAdapter adapter) {
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
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
	public void testChannelInstanceResolutionByPayloadConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		this.doTestChannelInstanceResolutionByPayload(adapter, channelRegistry);
	}

	@Test
	public void testChannelInstanceResolutionByPayloadConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, "routePayload");
		this.doTestChannelInstanceResolutionByPayload(adapter, channelRegistry);
	}

	private void doTestChannelInstanceResolutionByPayload(RouterMessageHandlerAdapter adapter, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
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
	public void testChannelInstanceResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		this.doTestChannelInstanceResolutionByMessage(adapter, channelRegistry);
	}

	@Test
	public void testChannelInstanceResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, "routeMessage");
		this.doTestChannelInstanceResolutionByMessage(adapter, channelRegistry);
	}

	private void doTestChannelInstanceResolutionByMessage(RouterMessageHandlerAdapter adapter, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
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
	public void testMultiChannelNameResolutionByPayloadConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		this.doTestMultiChannelNameResolutionByPayload(adapter, channelRegistry);
	}

	@Test
	public void testMultiChannelNameResolutionByPayloadConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, "routePayload");
		this.doTestMultiChannelNameResolutionByPayload(adapter, channelRegistry);
	}

	private void doTestMultiChannelNameResolutionByPayload(RouterMessageHandlerAdapter adapter, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
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
	public void testMultiChannelNameResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		this.doTestMultiChannelNameResolutionByMessage(adapter, channelRegistry);
	}

	@Test
	public void testMultiChannelNameResolutionByMessageConfiguredByMethodName() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, "routeMessage");
		this.doTestMultiChannelNameResolutionByMessage(adapter, channelRegistry);
	}

	private void doTestMultiChannelNameResolutionByMessage(RouterMessageHandlerAdapter adapter, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
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
	public void testMultiChannelNameArrayResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		this.doTestMultiChannelNameArrayResolutionByMessage(adapter, channelRegistry);
	}

	@Test
	public void testMultiChannelNameArrayResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, "routeMessageToArray");
		this.doTestMultiChannelNameArrayResolutionByMessage(adapter, channelRegistry);
	}

	private void doTestMultiChannelNameArrayResolutionByMessage(RouterMessageHandlerAdapter adapter, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
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
	public void testMultiChannelListResolutionByPayloadConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		this.doTestMultiChannelListResolutionByPayload(adapter, channelRegistry);
	}

	@Test
	public void testMultiChannelListResolutionByPayloadConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, "routePayload");
		this.doTestMultiChannelListResolutionByPayload(adapter, channelRegistry);
	}

	private void doTestMultiChannelListResolutionByPayload(RouterMessageHandlerAdapter adapter, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
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
	public void testMultiChannelListResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		this.doTestMultiChannelListResolutionByMessage(adapter, channelRegistry);
	}

	@Test
	public void testMultiChannelListResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, "routeMessage");
		this.doTestMultiChannelListResolutionByMessage(adapter, channelRegistry);
	}

	private void doTestMultiChannelListResolutionByMessage(RouterMessageHandlerAdapter adapter, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
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
	public void testMultiChannelArrayResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		this.doTestMultiChannelArrayResolutionByMessage(adapter, channelRegistry);
	}

	@Test
	public void testMultiChannelArrayResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, "routeMessageToArray");
		this.doTestMultiChannelArrayResolutionByMessage(adapter, channelRegistry);
	}

	private void doTestMultiChannelArrayResolutionByMessage(RouterMessageHandlerAdapter adapter, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		channelRegistry.registerChannel("bar-channel", barChannel);
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
	public void testChannelRegistryAwareTarget() throws Exception {
		QueueChannel fooChannel = new QueueChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("foo-channel", fooChannel);
		ChannelRegistryAwareTestBean testBean = new ChannelRegistryAwareTestBean();
		Method routingMethod = testBean.getClass().getMethod("route", String.class);
		RouterMessageHandlerAdapter adapter = new RouterMessageHandlerAdapter(testBean, routingMethod);
		adapter.setChannelRegistry(channelRegistry);
		assertNull(testBean.getChannelRegistry());
		adapter.afterPropertiesSet();
		assertNotNull(testBean.getChannelRegistry());
		Message<String> message = new StringMessage("foo-channel");
		adapter.handle(message);
		Message<?> result = fooChannel.receive(0);
		assertNotNull(result);
		assertEquals("foo-channel", result.getPayload());
	}


	public static class SingleChannelNameRoutingTestBean {

		public String routePayload(String name) {
			return name + "-channel";
		}

		public String routeByProperty(@HeaderProperty("returnAddress") String name) {
			return name + "-channel";
		}

		public String routeByAttribute(@HeaderAttribute("returnAddress") String name) {
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


	public static class ChannelRegistryAwareTestBean implements ChannelRegistryAware {

		private ChannelRegistry channelRegistry;

		public void setChannelRegistry(ChannelRegistry channelRegistry) {
			this.channelRegistry = channelRegistry;
		}

		public ChannelRegistry getChannelRegistry() {
			return this.channelRegistry;
		}

		public MessageChannel route(String channelName) {
			return this.channelRegistry.lookupChannel(channelName);
		}
	}

	public static class InvalidRoutingTestBean {

		public String tooManyAnnotations(@HeaderProperty("foo") @HeaderAttribute("bar") String name) {
			return name + "-channel";
		}

	}

}
