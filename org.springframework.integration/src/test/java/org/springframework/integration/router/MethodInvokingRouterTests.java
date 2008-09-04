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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MethodInvokingRouterTests {

	@Test
	public void channelNameResolutionByPayloadConfiguredByMethodReference() throws Exception {
		QueueChannel barChannel = new QueueChannel();
		barChannel.setBeanName("bar-channel");
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(barChannel);		
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> message = new GenericMessage<String>("bar");
		assertTrue(endpoint.send(message));
		Message<?> replyMessage = barChannel.receive();
		assertNotNull(replyMessage);
		assertEquals(message, replyMessage);
	}

	@Test
	public void channelNameResolutionByPayloadConfiguredByMethodName() {
		QueueChannel barChannel = new QueueChannel();
		barChannel.setBeanName("bar-channel");
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(barChannel);		
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, "routePayload");
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> message = new GenericMessage<String>("bar");
		assertTrue(endpoint.send(message));
		Message<?> replyMessage = barChannel.receive();
		assertNotNull(replyMessage);
		assertEquals(message, replyMessage);
	}

	@Test
	public void channelNameResolutionByHeader() throws Exception {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeByHeader", String.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> message = MessageBuilder.fromPayload("bar")
				.setHeader("targetChannel", "foo").build();
		assertTrue(endpoint.send(message));
		Message<?> fooReply = fooChannel.receive(0);
		Message<?> barReply = barChannel.receive(0);
		assertNotNull(fooReply);
		assertNull(barReply);
		assertEquals(message, fooReply);
	}

	@Test(expected = MessagingException.class)
	public void failsWhenRequiredHeaderIsNotProvided() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeByHeader", String.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.send(new GenericMessage<String>("testing"));
	}

	@Test
	public void channelNameResolutionByMessageConfiguredByMethodReference() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestChannelNameResolutionByMessage(endpoint);
	}

	@Test
	public void channelNameResolutionByMessageConfiguredByMethodName() {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, "routeMessage");
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestChannelNameResolutionByMessage(endpoint);
	}

	private void doTestChannelNameResolutionByMessage(RouterEndpoint endpoint) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		assertTrue(endpoint.send(fooMessage));
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		assertTrue(endpoint.send(barMessage));
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		assertFalse(endpoint.send(badMessage));
	}

	@Test
	public void channelInstanceResolutionByPayloadConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestChannelInstanceResolutionByPayload(endpoint, channelRegistry);
	}

	@Test
	public void channelInstanceResolutionByPayloadConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, "routePayload");
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestChannelInstanceResolutionByPayload(endpoint, channelRegistry);
	}

	private void doTestChannelInstanceResolutionByPayload(RouterEndpoint endpoint, ChannelRegistry channelRegistry) {
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		endpoint.setChannelRegistry(channelRegistry);
		assertTrue(endpoint.send(fooMessage));
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		assertTrue(endpoint.send(barMessage));
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		assertFalse(endpoint.send(badMessage));
	}

	@Test
	public void channelInstanceResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestChannelInstanceResolutionByMessage(endpoint, channelRegistry);
	}

	@Test
	public void channelInstanceResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, "routeMessage");
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestChannelInstanceResolutionByMessage(endpoint, channelRegistry);
	}

	private void doTestChannelInstanceResolutionByMessage(RouterEndpoint endpoint, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		assertTrue(endpoint.send(fooMessage));
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		assertTrue(endpoint.send(barMessage));
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		assertFalse(endpoint.send(badMessage));
	}

	@Test
	public void multiChannelNameResolutionByPayloadConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelNameResolutionByPayload(endpoint, channelRegistry);
	}

	@Test
	public void multiChannelNameResolutionByPayloadConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, "routePayload");
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelNameResolutionByPayload(endpoint, channelRegistry);
	}

	private void doTestMultiChannelNameResolutionByPayload(RouterEndpoint endpoint, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		assertTrue(endpoint.send(fooMessage));
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertTrue(endpoint.send(barMessage));
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertFalse(endpoint.send(badMessage));
	}

	@Test
	public void multiChannelNameResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelNameResolutionByMessage(endpoint, channelRegistry);
	}

	@Test
	public void multiChannelNameResolutionByMessageConfiguredByMethodName() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, "routeMessage");
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelNameResolutionByMessage(endpoint, channelRegistry);
	}

	private void doTestMultiChannelNameResolutionByMessage(RouterEndpoint endpoint, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		assertTrue(endpoint.send(fooMessage));
		Message<?> result1a = fooChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertTrue(endpoint.send(barMessage));
		Message<?> result2a = fooChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertFalse(endpoint.send(badMessage));
	}

	@Test
	public void multiChannelNameArrayResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelNameArrayResolutionByMessage(endpoint, channelRegistry);
	}

	@Test
	public void multiChannelNameArrayResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, "routeMessageToArray");
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelNameArrayResolutionByMessage(endpoint, channelRegistry);
	}

	private void doTestMultiChannelNameArrayResolutionByMessage(RouterEndpoint endpoint, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		assertTrue(endpoint.send(fooMessage));
		Message<?> result1a = fooChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertTrue(endpoint.send(barMessage));
		Message<?> result2a = fooChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertFalse(endpoint.send(badMessage));
	}

	@Test
	public void multiChannelListResolutionByPayloadConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelListResolutionByPayload(endpoint, channelRegistry);
	}

	@Test
	public void multiChannelListResolutionByPayloadConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, "routePayload");
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelListResolutionByPayload(endpoint, channelRegistry);
	}

	private void doTestMultiChannelListResolutionByPayload(RouterEndpoint endpoint, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		assertTrue(endpoint.send(fooMessage));
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertTrue(endpoint.send(barMessage));
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertFalse(endpoint.send(badMessage));
	}

	@Test
	public void multiChannelListResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelListResolutionByMessage(endpoint, channelRegistry);
	}

	@Test
	public void multiChannelListResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, "routeMessage");
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelListResolutionByMessage(endpoint, channelRegistry);
	}

	private void doTestMultiChannelListResolutionByMessage(RouterEndpoint endpoint, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		assertTrue(endpoint.send(fooMessage));
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertTrue(endpoint.send(barMessage));
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertFalse(endpoint.send(badMessage));
	}

	@Test
	public void multiChannelArrayResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, routingMethod);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelArrayResolutionByMessage(endpoint, channelRegistry);
	}

	@Test
	public void multiChannelArrayResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		MethodInvokingChannelResolver resolver = new MethodInvokingChannelResolver(testBean, "routeMessageToArray");
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		this.doTestMultiChannelArrayResolutionByMessage(endpoint, channelRegistry);
	}

	private void doTestMultiChannelArrayResolutionByMessage(RouterEndpoint endpoint, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		endpoint.setChannelRegistry(channelRegistry);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		assertTrue(endpoint.send(fooMessage));
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertTrue(endpoint.send(barMessage));
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertFalse(endpoint.send(badMessage));
	}


	public static class SingleChannelNameRoutingTestBean {

		public String routePayload(String name) {
			return name + "-channel";
		}

		public String routeByHeader(@Header("targetChannel") String name) {
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

}
