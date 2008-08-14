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

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.annotation.Header;
import org.springframework.integration.message.CompositeMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RouterMessageHandlerTests {

	@Test
	public void channelNameResolutionByPayloadConfiguredByMethodReference() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		Message<String> message = new GenericMessage<String>("bar");
		Message<?> reply = handler.handle(message);
		assertNotNull(reply);
		assertEquals(CompositeMessage.class, reply.getClass());
		List<Message<?>> replyMessages = ((CompositeMessage) reply).getPayload();
		assertEquals(1, replyMessages.size());
		Message<?> replyMessage = replyMessages.get(0);
		assertEquals("bar", replyMessage.getPayload());
		assertEquals("bar-channel", replyMessage.getHeaders().getNextTarget());
	}

	@Test
	public void channelNameResolutionByPayloadConfiguredByMethodName() {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		RouterMessageHandler handler = new RouterMessageHandler(testBean, "routePayload");
		Message<String> message = new GenericMessage<String>("bar");
		Message<?> reply = handler.handle(message);
		assertNotNull(reply);
		assertNotNull(reply);
		assertEquals(CompositeMessage.class, reply.getClass());
		List<Message<?>> replyMessages = ((CompositeMessage) reply).getPayload();
		assertEquals(1, replyMessages.size());
		Message<?> replyMessage = replyMessages.get(0);
		assertEquals("bar", replyMessage.getPayload());
		assertEquals("bar-channel", replyMessage.getHeaders().getNextTarget());
	}

	@Test
	public void channelNameResolutionByHeader() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeByHeader", String.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		Message<String> message = MessageBuilder.fromPayload("bar")
				.setHeader("targetChannel", "foo").build();
		Message<?> reply = handler.handle(message);
		assertNotNull(reply);
		assertEquals(CompositeMessage.class, reply.getClass());
		List<Message<?>> replyMessages = ((CompositeMessage) reply).getPayload();
		assertEquals(1, replyMessages.size());
		Message<?> replyMessage = replyMessages.get(0);
		assertEquals("bar", replyMessage.getPayload());
		assertEquals("foo-channel", replyMessage.getHeaders().getNextTarget());
	}

	@Test(expected=MessageHandlingException.class)
	public void failsWhenRequireddHeaderIsNotProvided() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeByHeader", String.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		handler.handle(new GenericMessage<String>("testing"));
	}

	@Test
	public void channelNameResolutionByMessageConfiguredByMethodReference() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		this.doTestChannelNameResolutionByMessage(handler);
	}

	@Test
	public void channelNameResolutionByMessageConfiguredByMethodName() {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		RouterMessageHandler handler = new RouterMessageHandler(testBean, "routeMessage");
		this.doTestChannelNameResolutionByMessage(handler);
	}

	private void doTestChannelNameResolutionByMessage(RouterMessageHandler handler) {
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		Message<?> result1 = ((CompositeMessage) handler.handle(fooMessage)).getPayload().get(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		Message<?> result2 = ((CompositeMessage) handler.handle(barMessage)).getPayload().get(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		assertNull(handler.handle(badMessage));
	}

	@Test
	public void channelInstanceResolutionByPayloadConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		this.doTestChannelInstanceResolutionByPayload(handler, channelRegistry);
	}

	@Test
	public void channelInstanceResolutionByPayloadConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, "routePayload");
		this.doTestChannelInstanceResolutionByPayload(handler, channelRegistry);
	}

	private void doTestChannelInstanceResolutionByPayload(RouterMessageHandler handler, ChannelRegistry channelRegistry) {
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		Message<?> result1 = ((CompositeMessage) handler.handle(fooMessage)).getPayload().get(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		assertEquals(fooChannel, result1.getHeaders().getNextTarget());
		Message<?> result2 = ((CompositeMessage) handler.handle(barMessage)).getPayload().get(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		assertEquals(barChannel, result2.getHeaders().getNextTarget());
		assertNull(handler.handle(badMessage));
	}

	@Test
	public void channelInstanceResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		this.doTestChannelInstanceResolutionByMessage(handler, channelRegistry);
	}

	@Test
	public void channelInstanceResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelRegistry);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, "routeMessage");
		this.doTestChannelInstanceResolutionByMessage(handler, channelRegistry);
	}

	private void doTestChannelInstanceResolutionByMessage(RouterMessageHandler handler, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		Message<?> result1 = ((CompositeMessage) handler.handle(fooMessage)).getPayload().get(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		assertEquals(fooChannel, result1.getHeaders().getNextTarget());
		Message<?> result2 = ((CompositeMessage) handler.handle(barMessage)).getPayload().get(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		assertEquals(barChannel, result2.getHeaders().getNextTarget());
		assertNull(handler.handle(badMessage));
	}

	@Test
	public void multiChannelNameResolutionByPayloadConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		this.doTestMultiChannelNameResolutionByPayload(handler, channelRegistry);
	}

	@Test
	public void multiChannelNameResolutionByPayloadConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		RouterMessageHandler handler = new RouterMessageHandler(testBean, "routePayload");
		this.doTestMultiChannelNameResolutionByPayload(handler, channelRegistry);
	}

	private void doTestMultiChannelNameResolutionByPayload(RouterMessageHandler handler, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		CompositeMessage reply1 = (CompositeMessage) handler.handle(fooMessage);
		Message<?> result1a = reply1.getPayload().get(0);
		Message<?> result1b = reply1.getPayload().get(1);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());		
		assertEquals("foo-channel", result1a.getHeaders().getNextTarget());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertEquals("bar-channel", result1b.getHeaders().getNextTarget());
		CompositeMessage reply2 = (CompositeMessage) handler.handle(barMessage);
		Message<?> result2a = reply2.getPayload().get(0);
		Message<?> result2b = reply2.getPayload().get(1);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertEquals("foo-channel", result2a.getHeaders().getNextTarget());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertEquals("bar-channel", result2b.getHeaders().getNextTarget());
		assertNull(handler.handle(badMessage));
	}

	@Test
	public void multiChannelNameResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		this.doTestMultiChannelNameResolutionByMessage(handler, channelRegistry);
	}

	@Test
	public void multiChannelNameResolutionByMessageConfiguredByMethodName() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		RouterMessageHandler handler = new RouterMessageHandler(testBean, "routeMessage");
		this.doTestMultiChannelNameResolutionByMessage(handler, channelRegistry);
	}

	private void doTestMultiChannelNameResolutionByMessage(RouterMessageHandler handler, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		CompositeMessage reply1 = (CompositeMessage) handler.handle(fooMessage);
		Message<?> result1a = reply1.getPayload().get(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertEquals("foo-channel", result1a.getHeaders().getNextTarget());
		Message<?> result1b = reply1.getPayload().get(1);
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertEquals("bar-channel", result1b.getHeaders().getNextTarget());
		CompositeMessage reply2 = (CompositeMessage) handler.handle(barMessage);
		Message<?> result2a = reply2.getPayload().get(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertEquals("foo-channel", result2a.getHeaders().getNextTarget());
		Message<?> result2b = reply2.getPayload().get(1);
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertEquals("bar-channel", result2b.getHeaders().getNextTarget());
		assertNull(handler.handle(badMessage));
	}

	@Test
	public void testMultiChannelNameArrayResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		this.doTestMultiChannelNameArrayResolutionByMessage(handler, channelRegistry);
	}

	@Test
	public void testMultiChannelNameArrayResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		RouterMessageHandler handler = new RouterMessageHandler(testBean, "routeMessageToArray");
		this.doTestMultiChannelNameArrayResolutionByMessage(handler, channelRegistry);
	}

	private void doTestMultiChannelNameArrayResolutionByMessage(RouterMessageHandler handler, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		CompositeMessage reply1 = (CompositeMessage) handler.handle(fooMessage);
		Message<?> result1a = reply1.getPayload().get(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertEquals("foo-channel", result1a.getHeaders().getNextTarget());
		Message<?> result1b = reply1.getPayload().get(1);
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertEquals("bar-channel", result1b.getHeaders().getNextTarget());
		CompositeMessage reply2 = (CompositeMessage) handler.handle(barMessage);
		Message<?> result2a = reply2.getPayload().get(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertEquals("foo-channel", result2a.getHeaders().getNextTarget());
		Message<?> result2b = reply2.getPayload().get(1);
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertEquals("bar-channel", result2b.getHeaders().getNextTarget());
		assertNull(handler.handle(badMessage));
	}

	@Test
	public void testMultiChannelListResolutionByPayloadConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		this.doTestMultiChannelListResolutionByPayload(handler, channelRegistry);
	}

	@Test
	public void testMultiChannelListResolutionByPayloadConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, "routePayload");
		this.doTestMultiChannelListResolutionByPayload(handler, channelRegistry);
	}

	private void doTestMultiChannelListResolutionByPayload(RouterMessageHandler handler, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		CompositeMessage reply1 = (CompositeMessage) handler.handle(fooMessage);
		Message<?> result1a = reply1.getPayload().get(0);
		Message<?> result1b = reply1.getPayload().get(1);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertEquals(fooChannel, result1a.getHeaders().getNextTarget());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertEquals(barChannel, result1b.getHeaders().getNextTarget());
		CompositeMessage reply2 = (CompositeMessage) handler.handle(barMessage);
		Message<?> result2a = reply2.getPayload().get(0);
		Message<?> result2b = reply2.getPayload().get(1);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertEquals(fooChannel, result2a.getHeaders().getNextTarget());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertEquals(barChannel, result2b.getHeaders().getNextTarget());
		assertNull(handler.handle(badMessage));
	}

	@Test
	public void testMultiChannelListResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		this.doTestMultiChannelListResolutionByMessage(handler, channelRegistry);
	}

	@Test
	public void testMultiChannelListResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, "routeMessage");
		this.doTestMultiChannelListResolutionByMessage(handler, channelRegistry);
	}

	private void doTestMultiChannelListResolutionByMessage(RouterMessageHandler handler, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		CompositeMessage reply1 = (CompositeMessage) handler.handle(fooMessage);
		Message<?> result1a = reply1.getPayload().get(0);
		Message<?> result1b = reply1.getPayload().get(1);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertEquals(fooChannel, result1a.getHeaders().getNextTarget());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertEquals(barChannel, result1b.getHeaders().getNextTarget());
		CompositeMessage reply2 = (CompositeMessage) handler.handle(barMessage);
		Message<?> result2a = reply2.getPayload().get(0);
		Message<?> result2b = reply2.getPayload().get(1);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertEquals(fooChannel, result2a.getHeaders().getNextTarget());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertEquals(barChannel, result2b.getHeaders().getNextTarget());
		assertNull(handler.handle(badMessage));
	}

	@Test
	public void testMultiChannelArrayResolutionByMessageConfiguredByMethodReference() throws Exception {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, routingMethod);
		this.doTestMultiChannelArrayResolutionByMessage(handler, channelRegistry);
	}

	@Test
	public void testMultiChannelArrayResolutionByMessageConfiguredByMethodName() {
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelRegistry);
		RouterMessageHandler handler = new RouterMessageHandler(testBean, "routeMessageToArray");
		this.doTestMultiChannelArrayResolutionByMessage(handler, channelRegistry);
	}

	private void doTestMultiChannelArrayResolutionByMessage(RouterMessageHandler handler, ChannelRegistry channelRegistry) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelRegistry.registerChannel(fooChannel);
		channelRegistry.registerChannel(barChannel);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		CompositeMessage reply1 = (CompositeMessage) handler.handle(fooMessage);
		Message<?> result1a = reply1.getPayload().get(0);
		Message<?> result1b = reply1.getPayload().get(1);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertEquals(fooChannel, result1a.getHeaders().getNextTarget());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		assertEquals(barChannel, result1b.getHeaders().getNextTarget());
		CompositeMessage reply2 = (CompositeMessage) handler.handle(barMessage);
		Message<?> result2a = reply2.getPayload().get(0);
		Message<?> result2b = reply2.getPayload().get(1);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertEquals(fooChannel, result2a.getHeaders().getNextTarget());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		assertEquals(barChannel, result2b.getHeaders().getNextTarget());
		assertNull(handler.handle(badMessage));
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
