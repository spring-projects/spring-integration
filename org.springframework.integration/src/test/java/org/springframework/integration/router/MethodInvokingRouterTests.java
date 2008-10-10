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

import org.springframework.integration.annotation.Header;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.TestChannelMapping;
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
		TestChannelMapping channelMapping = new TestChannelMapping();
		channelMapping.addChannel(barChannel);		
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		router.setChannelMapping(channelMapping);
		Message<String> message = new GenericMessage<String>("bar");
		router.onMessage(message);
		Message<?> replyMessage = barChannel.receive();
		assertNotNull(replyMessage);
		assertEquals(message, replyMessage);
	}

	@Test
	public void channelNameResolutionByPayloadConfiguredByMethodName() {
		QueueChannel barChannel = new QueueChannel();
		barChannel.setBeanName("bar-channel");
		TestChannelMapping channelMapping = new TestChannelMapping();
		channelMapping.addChannel(barChannel);		
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routePayload");
		router.setChannelMapping(channelMapping);
		Message<String> message = new GenericMessage<String>("bar");
		router.onMessage(message);
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
		TestChannelMapping channelMapping = new TestChannelMapping();
		channelMapping.addChannel(fooChannel);
		channelMapping.addChannel(barChannel);
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeByHeader", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		router.setChannelMapping(channelMapping);
		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader("targetChannel", "foo").build();
		router.onMessage(message);
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
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		router.onMessage(new GenericMessage<String>("testing"));
	}

	@Test
	public void channelNameResolutionByMessageConfiguredByMethodReference() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		this.doTestChannelNameResolutionByMessage(router);
	}

	@Test
	public void channelNameResolutionByMessageConfiguredByMethodName() {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessage");
		this.doTestChannelNameResolutionByMessage(router);
	}

	private void doTestChannelNameResolutionByMessage(MethodInvokingRouter router) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		TestChannelMapping channelMapping = new TestChannelMapping();
		channelMapping.addChannel(fooChannel);
		channelMapping.addChannel(barChannel);
		router.setChannelMapping(channelMapping);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		router.onMessage(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		router.onMessage(barMessage);
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		router.onMessage(badMessage);
	}

	@Test
	public void channelInstanceResolutionByPayloadConfiguredByMethodReference() throws Exception {
		TestChannelMapping channelMapping = new TestChannelMapping();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelMapping);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		this.doTestChannelInstanceResolutionByPayload(router, channelMapping);
	}

	@Test
	public void channelInstanceResolutionByPayloadConfiguredByMethodName() {
		TestChannelMapping channelMapping = new TestChannelMapping();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelMapping);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routePayload");
		this.doTestChannelInstanceResolutionByPayload(router, channelMapping);
	}

	private void doTestChannelInstanceResolutionByPayload(MethodInvokingRouter router, TestChannelMapping channelMapping) {
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelMapping.addChannel(fooChannel);
		channelMapping.addChannel(barChannel);
		router.setChannelMapping(channelMapping);
		router.onMessage(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		router.onMessage(barMessage);
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		router.onMessage(badMessage);
	}

	@Test
	public void channelInstanceResolutionByMessageConfiguredByMethodReference() throws Exception {
		TestChannelMapping channelMapping = new TestChannelMapping();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelMapping);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		this.doTestChannelInstanceResolutionByMessage(router, channelMapping);
	}

	@Test
	public void channelInstanceResolutionByMessageConfiguredByMethodName() {
		TestChannelMapping channelMapping = new TestChannelMapping();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelMapping);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessage");
		this.doTestChannelInstanceResolutionByMessage(router, channelMapping);
	}

	private void doTestChannelInstanceResolutionByMessage(MethodInvokingRouter router, TestChannelMapping channelMapping) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelMapping.addChannel(fooChannel);
		channelMapping.addChannel(barChannel);
		router.setChannelMapping(channelMapping);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		router.onMessage(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		router.onMessage(barMessage);
		Message<?> result2 = barChannel.receive(0);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
		router.onMessage(badMessage);
	}

	@Test
	public void multiChannelNameResolutionByPayloadConfiguredByMethodReference() throws Exception {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		this.doTestMultiChannelNameResolutionByPayload(router, channelMapping);
	}

	@Test
	public void multiChannelNameResolutionByPayloadConfiguredByMethodName() {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routePayload");
		this.doTestMultiChannelNameResolutionByPayload(router, channelMapping);
	}

	private void doTestMultiChannelNameResolutionByPayload(MethodInvokingRouter router, TestChannelMapping channelMapping) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelMapping.addChannel(fooChannel);
		channelMapping.addChannel(barChannel);
		router.setChannelMapping(channelMapping);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		router.onMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		router.onMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		router.onMessage(badMessage);
	}

	@Test
	public void multiChannelNameResolutionByMessageConfiguredByMethodReference() throws Exception {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		this.doTestMultiChannelNameResolutionByMessage(router, channelMapping);
	}

	@Test
	public void multiChannelNameResolutionByMessageConfiguredByMethodName() throws Exception {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessage");
		this.doTestMultiChannelNameResolutionByMessage(router, channelMapping);
	}

	private void doTestMultiChannelNameResolutionByMessage(MethodInvokingRouter router, TestChannelMapping channelMapping) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelMapping.addChannel(fooChannel);
		channelMapping.addChannel(barChannel);
		router.setChannelMapping(channelMapping);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		router.onMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		router.onMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		router.onMessage(badMessage);
	}

	@Test
	public void multiChannelNameArrayResolutionByMessageConfiguredByMethodReference() throws Exception {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		this.doTestMultiChannelNameArrayResolutionByMessage(router, channelMapping);
	}

	@Test
	public void multiChannelNameArrayResolutionByMessageConfiguredByMethodName() {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessageToArray");
		this.doTestMultiChannelNameArrayResolutionByMessage(router, channelMapping);
	}

	private void doTestMultiChannelNameArrayResolutionByMessage(MethodInvokingRouter router, TestChannelMapping channelMapping) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelMapping.addChannel(fooChannel);
		channelMapping.addChannel(barChannel);
		router.setChannelMapping(channelMapping);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		router.onMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		router.onMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		router.onMessage(badMessage);
	}

	@Test
	public void multiChannelListResolutionByPayloadConfiguredByMethodReference() throws Exception {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelMapping);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		this.doTestMultiChannelListResolutionByPayload(router, channelMapping);
	}

	@Test
	public void multiChannelListResolutionByPayloadConfiguredByMethodName() {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelMapping);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routePayload");
		this.doTestMultiChannelListResolutionByPayload(router, channelMapping);
	}

	private void doTestMultiChannelListResolutionByPayload(MethodInvokingRouter router, TestChannelMapping channelMapping) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelMapping.addChannel(fooChannel);
		channelMapping.addChannel(barChannel);
		router.setChannelMapping(channelMapping);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		router.onMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		router.onMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		router.onMessage(badMessage);
	}

	@Test
	public void multiChannelListResolutionByMessageConfiguredByMethodReference() throws Exception {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelMapping);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		this.doTestMultiChannelListResolutionByMessage(router, channelMapping);
	}

	@Test
	public void multiChannelListResolutionByMessageConfiguredByMethodName() {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelMapping);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessage");
		this.doTestMultiChannelListResolutionByMessage(router, channelMapping);
	}

	private void doTestMultiChannelListResolutionByMessage(MethodInvokingRouter router, TestChannelMapping channelMapping) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelMapping.addChannel(fooChannel);
		channelMapping.addChannel(barChannel);
		router.setChannelMapping(channelMapping);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		router.onMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		router.onMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		router.onMessage(badMessage);
	}

	@Test
	public void multiChannelArrayResolutionByMessageConfiguredByMethodReference() throws Exception {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelMapping);
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		this.doTestMultiChannelArrayResolutionByMessage(router, channelMapping);
	}

	@Test
	public void multiChannelArrayResolutionByMessageConfiguredByMethodName() {
		TestChannelMapping channelMapping = new TestChannelMapping();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelMapping);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessageToArray");
		this.doTestMultiChannelArrayResolutionByMessage(router, channelMapping);
	}

	private void doTestMultiChannelArrayResolutionByMessage(MethodInvokingRouter router, TestChannelMapping channelMapping) {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		fooChannel.setBeanName("foo-channel");
		barChannel.setBeanName("bar-channel");
		channelMapping.addChannel(fooChannel);
		channelMapping.addChannel(barChannel);
		router.setChannelMapping(channelMapping);
		Message<String> fooMessage = new StringMessage("foo");
		Message<String> barMessage = new StringMessage("bar");
		Message<String> badMessage = new StringMessage("bad");
		router.onMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		router.onMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		router.onMessage(badMessage);
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

		private ChannelMapping mapping;

		public SingleChannelInstanceRoutingTestBean(ChannelMapping mapping) {
			this.mapping = mapping;
		}

		public MessageChannel routePayload(String name) {
			return mapping.getChannel(name + "-channel");
		}

		public MessageChannel routeMessage(Message<?> message) {
			if (message.getPayload().equals("foo")) {
				return mapping.getChannel("foo-channel");
			}
			else if (message.getPayload().equals("bar")) {
				return mapping.getChannel("bar-channel");
			}
			return null;
		}
	}


	public static class MultiChannelInstanceRoutingTestBean {

		private ChannelMapping mapping;

		public MultiChannelInstanceRoutingTestBean(ChannelMapping mapping) {
			this.mapping = mapping;
		}

		public List<MessageChannel> routePayload(String name) {
			List<MessageChannel> results = new ArrayList<MessageChannel>();
			if (name.equals("foo") || name.equals("bar")) {
				results.add(mapping.getChannel("foo-channel"));
				results.add(mapping.getChannel("bar-channel"));
			}
			return results;
		}

		public List<MessageChannel> routeMessage(Message<?> message) {
			List<MessageChannel> results = new ArrayList<MessageChannel>();
			if (message.getPayload().equals("foo") || message.getPayload().equals("bar")) {
				results.add(mapping.getChannel("foo-channel"));
				results.add(mapping.getChannel("bar-channel"));
			}
			return results;
		}

		public MessageChannel[] routeMessageToArray(Message<?> message) {
			MessageChannel[] results = null;
			if (message.getPayload().equals("foo") || message.getPayload().equals("bar")) {
				results = new MessageChannel[2];
				results[0] = mapping.getChannel("foo-channel");
				results[1] = mapping.getChannel("bar-channel");
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
