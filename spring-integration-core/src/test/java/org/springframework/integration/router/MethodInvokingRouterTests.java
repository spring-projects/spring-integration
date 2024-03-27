/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.router;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelResolver;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MethodInvokingRouterTests {

	@Test
	public void channelNameResolutionByPayloadConfiguredByMethodReference() throws Exception {
		QueueChannel barChannel = new QueueChannel();
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel("bar-channel", barChannel);
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		router.setChannelResolver(channelResolver);
		Message<String> message = new GenericMessage<>("bar");
		router.handleMessage(message);
		Message<?> replyMessage = barChannel.receive();
		assertThat(replyMessage).isNotNull();
		assertThat(replyMessage).isEqualTo(message);
	}

	@Test
	public void channelNameResolutionByPayloadConfiguredByMethodName() {
		QueueChannel barChannel = new QueueChannel();
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel("bar-channel", barChannel);
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routePayload");
		router.setChannelResolver(channelResolver);
		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		Message<String> message = new GenericMessage<>("bar");
		router.handleMessage(message);
		Message<?> replyMessage = barChannel.receive();
		assertThat(replyMessage).isNotNull();
		assertThat(replyMessage).isEqualTo(message);
	}

	@Test
	public void channelNameResolutionByHeader() throws Exception {
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel("foo-channel", fooChannel);
		channelResolver.addChannel("bar-channel", barChannel);
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeByHeader", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		router.setChannelResolver(channelResolver);
		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader("targetChannel", "foo").build();
		router.handleMessage(message);
		Message<?> fooReply = fooChannel.receive(0);
		Message<?> barReply = barChannel.receive(0);
		assertThat(fooReply).isNotNull();
		assertThat(barReply).isNull();
		assertThat(fooReply).isEqualTo(message);
	}

	@Test(expected = MessagingException.class)
	public void failsWhenRequiredHeaderIsNotProvided() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeByHeader", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		router.handleMessage(new GenericMessage<String>("testing"));
	}

	@Test
	public void channelNameResolutionByMessageConfiguredByMethodReference() throws Exception {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		doTestChannelNameResolutionByMessage(router);
	}

	@Test
	public void channelNameResolutionByMessageConfiguredByMethodName() {
		SingleChannelNameRoutingTestBean testBean = new SingleChannelNameRoutingTestBean();
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessage");
		doTestChannelNameResolutionByMessage(router);
	}

	private void doTestChannelNameResolutionByMessage(MethodInvokingRouter router) {
		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel("foo-channel", fooChannel);
		channelResolver.addChannel("bar-channel", barChannel);
		router.setChannelResolver(channelResolver);
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");
		router.handleMessage(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("foo");
		router.handleMessage(barMessage);
		Message<?> result2 = barChannel.receive(0);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isEqualTo("bar");

		try {
			router.handleMessage(badMessage);
			fail("MessageDeliveryException expected");
		}
		catch (MessageDeliveryException e) {
			/* Success */
		}
	}

	@Test
	public void channelInstanceResolutionByPayloadConfiguredByMethodReference() throws Exception {
		TestChannelResolver channelResolver = new TestChannelResolver();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelResolver);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		doTestChannelInstanceResolutionByPayload(router, channelResolver);
	}

	@Test
	public void channelInstanceResolutionByPayloadConfiguredByMethodName() {
		TestChannelResolver channelResolver = new TestChannelResolver();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelResolver);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routePayload");
		doTestChannelInstanceResolutionByPayload(router, channelResolver);
	}

	private void doTestChannelInstanceResolutionByPayload(MethodInvokingRouter router,
			TestChannelResolver channelResolver) {

		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelResolver.addChannel("foo-channel", fooChannel);
		channelResolver.addChannel("bar-channel", barChannel);
		router.setChannelResolver(channelResolver);
		router.handleMessage(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("foo");
		router.handleMessage(barMessage);
		Message<?> result2 = barChannel.receive(0);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isEqualTo("bar");

		try {
			router.handleMessage(badMessage);
			fail("MessageDeliveryException expected");
		}
		catch (MessageDeliveryException e) {
			/* Success */
		}

	}

	@Test
	public void channelInstanceResolutionByMessageConfiguredByMethodReference() throws Exception {
		TestChannelResolver channelResolver = new TestChannelResolver();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelResolver);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		doTestChannelInstanceResolutionByMessage(router, channelResolver);
	}

	@Test
	public void channelInstanceResolutionByMessageConfiguredByMethodName() {
		TestChannelResolver channelResolver = new TestChannelResolver();
		SingleChannelInstanceRoutingTestBean testBean = new SingleChannelInstanceRoutingTestBean(channelResolver);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessage");
		doTestChannelInstanceResolutionByMessage(router, channelResolver);
	}

	private void doTestChannelInstanceResolutionByMessage(MethodInvokingRouter router,
			TestChannelResolver channelResolver) {

		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelResolver.addChannel("foo-channel", fooChannel);
		channelResolver.addChannel("bar-channel", barChannel);
		router.setChannelResolver(channelResolver);
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");
		router.handleMessage(fooMessage);
		Message<?> result1 = fooChannel.receive(0);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("foo");
		router.handleMessage(barMessage);
		Message<?> result2 = barChannel.receive(0);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isEqualTo("bar");

		try {
			router.handleMessage(badMessage);
			fail("MessageDeliveryException expected");
		}
		catch (MessageDeliveryException e) {
			/* Success */
		}

	}

	@Test
	public void multiChannelNameResolutionByPayloadConfiguredByMethodReference() throws Exception {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		doTestMultiChannelNameResolutionByPayload(router, channelResolver);
	}

	@Test
	public void multiChannelNameResolutionByPayloadConfiguredByMethodName() {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routePayload");
		doTestMultiChannelNameResolutionByPayload(router, channelResolver);
	}

	private void doTestMultiChannelNameResolutionByPayload(MethodInvokingRouter router,
			TestChannelResolver channelResolver) {

		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelResolver.addChannel("foo-channel", fooChannel);
		channelResolver.addChannel("bar-channel", barChannel);
		router.setChannelResolver(channelResolver);
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");
		router.handleMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		assertThat(result1b).isNotNull();
		assertThat(result1b.getPayload()).isEqualTo("foo");
		router.handleMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertThat(result2a).isNotNull();
		assertThat(result2a.getPayload()).isEqualTo("bar");
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");

		try {
			router.handleMessage(badMessage);
			fail("MessageDeliveryException expected");
		}
		catch (MessageDeliveryException e) {
			/* Success */
		}
	}

	@Test
	public void multiChannelNameResolutionByMessageConfiguredByMethodReference() throws Exception {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		doTestMultiChannelNameResolutionByMessage(router, channelResolver);
	}

	@Test
	public void multiChannelNameResolutionByMessageConfiguredByMethodName() {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessage");
		this.doTestMultiChannelNameResolutionByMessage(router, channelResolver);
	}

	private void doTestMultiChannelNameResolutionByMessage(MethodInvokingRouter router,
			TestChannelResolver channelResolver) {

		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelResolver.addChannel("foo-channel", fooChannel);
		channelResolver.addChannel("bar-channel", barChannel);
		router.setChannelResolver(channelResolver);
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");
		router.handleMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		Message<?> result1b = barChannel.receive(0);
		assertThat(result1b).isNotNull();
		assertThat(result1b.getPayload()).isEqualTo("foo");
		router.handleMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		assertThat(result2a).isNotNull();
		assertThat(result2a.getPayload()).isEqualTo("bar");
		Message<?> result2b = barChannel.receive(0);
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");

		try {
			router.handleMessage(badMessage);
			fail("MessageDeliveryException expected");
		}
		catch (MessageDeliveryException e) {
			/* Success */
		}
	}

	@Test
	public void multiChannelNameArrayResolutionByMessageConfiguredByMethodReference() throws Exception {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		doTestMultiChannelNameArrayResolutionByMessage(router, channelResolver);
	}

	@Test
	public void multiChannelNameArrayResolutionByMessageConfiguredByMethodName() {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelNameRoutingTestBean testBean = new MultiChannelNameRoutingTestBean();
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessageToArray");
		doTestMultiChannelNameArrayResolutionByMessage(router, channelResolver);
	}

	private void doTestMultiChannelNameArrayResolutionByMessage(MethodInvokingRouter router,
			TestChannelResolver channelResolver) {

		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelResolver.addChannel("foo-channel", fooChannel);
		channelResolver.addChannel("bar-channel", barChannel);
		router.setChannelResolver(channelResolver);
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");
		router.handleMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		Message<?> result1b = barChannel.receive(0);
		assertThat(result1b).isNotNull();
		assertThat(result1b.getPayload()).isEqualTo("foo");
		router.handleMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		assertThat(result2a).isNotNull();
		assertThat(result2a.getPayload()).isEqualTo("bar");
		Message<?> result2b = barChannel.receive(0);
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");

		try {
			router.handleMessage(badMessage);
			fail("MessageDeliveryException expected");
		}
		catch (MessageDeliveryException e) {
			/* Success */
		}
	}

	@Test
	public void multiChannelListResolutionByPayloadConfiguredByMethodReference() throws Exception {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelResolver);
		Method routingMethod = testBean.getClass().getMethod("routePayload", String.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		doTestMultiChannelListResolutionByPayload(router, channelResolver);
	}

	@Test
	public void multiChannelListResolutionByPayloadConfiguredByMethodName() {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelResolver);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routePayload");
		doTestMultiChannelListResolutionByPayload(router, channelResolver);
	}

	private void doTestMultiChannelListResolutionByPayload(MethodInvokingRouter router,
			TestChannelResolver channelResolver) {

		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelResolver.addChannel("foo-channel", fooChannel);
		channelResolver.addChannel("bar-channel", barChannel);
		router.setChannelResolver(channelResolver);
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");
		router.handleMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		assertThat(result1b).isNotNull();
		assertThat(result1b.getPayload()).isEqualTo("foo");
		router.handleMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertThat(result2a).isNotNull();
		assertThat(result2a.getPayload()).isEqualTo("bar");
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");

		try {
			router.handleMessage(badMessage);
			fail("MessageDeliveryException expected");
		}
		catch (MessageDeliveryException e) {
			/* Success */
		}

	}

	@Test
	public void multiChannelListResolutionByMessageConfiguredByMethodReference() throws Exception {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelResolver);
		Method routingMethod = testBean.getClass().getMethod("routeMessage", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		this.doTestMultiChannelListResolutionByMessage(router, channelResolver);
	}

	@Test
	public void multiChannelListResolutionByMessageConfiguredByMethodName() {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelResolver);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessage");
		doTestMultiChannelListResolutionByMessage(router, channelResolver);
	}

	private void doTestMultiChannelListResolutionByMessage(MethodInvokingRouter router,
			TestChannelResolver channelResolver) {

		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelResolver.addChannel("foo-channel", fooChannel);
		channelResolver.addChannel("bar-channel", barChannel);
		router.setChannelResolver(channelResolver);
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");
		router.handleMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		assertThat(result1b).isNotNull();
		assertThat(result1b.getPayload()).isEqualTo("foo");
		router.handleMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertThat(result2a).isNotNull();
		assertThat(result2a.getPayload()).isEqualTo("bar");
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");

		try {
			router.handleMessage(badMessage);
			fail("MessageDeliveryException expected");
		}
		catch (MessageDeliveryException e) {
			/* Success */
		}

	}

	@Test
	public void multiChannelArrayResolutionByMessageConfiguredByMethodReference() throws Exception {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelResolver);
		Method routingMethod = testBean.getClass().getMethod("routeMessageToArray", Message.class);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, routingMethod);
		doTestMultiChannelArrayResolutionByMessage(router, channelResolver);
	}

	@Test
	public void multiChannelArrayResolutionByMessageConfiguredByMethodName() {
		TestChannelResolver channelResolver = new TestChannelResolver();
		MultiChannelInstanceRoutingTestBean testBean = new MultiChannelInstanceRoutingTestBean(channelResolver);
		MethodInvokingRouter router = new MethodInvokingRouter(testBean, "routeMessageToArray");
		doTestMultiChannelArrayResolutionByMessage(router, channelResolver);
	}

	private void doTestMultiChannelArrayResolutionByMessage(MethodInvokingRouter router,
			TestChannelResolver channelResolver) {

		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		channelResolver.addChannel("foo-channel", fooChannel);
		channelResolver.addChannel("bar-channel", barChannel);
		router.setChannelResolver(channelResolver);
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");
		router.handleMessage(fooMessage);
		Message<?> result1a = fooChannel.receive(0);
		Message<?> result1b = barChannel.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		assertThat(result1b).isNotNull();
		assertThat(result1b.getPayload()).isEqualTo("foo");
		router.handleMessage(barMessage);
		Message<?> result2a = fooChannel.receive(0);
		Message<?> result2b = barChannel.receive(0);
		assertThat(result2a).isNotNull();
		assertThat(result2a.getPayload()).isEqualTo("bar");
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");

		try {
			router.handleMessage(badMessage);
			fail("MessageDeliveryException expected");
		}
		catch (MessageDeliveryException e) {
			/* Success */
		}

	}

	@Test
	public void testClassAsKeyResolution() {
		QueueChannel stringsChannel = new QueueChannel();
		QueueChannel numbersChannel = new QueueChannel();
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel("stringsChannel", stringsChannel);
		channelResolver.addChannel("numbersChannel", numbersChannel);

		MethodInvokingRouter router = new MethodInvokingRouter(new ClassAsKeyTestBean());
		router.setChannelResolver(channelResolver);
		router.setChannelMapping(String.class.getName(), "stringsChannel");
		router.setChannelMapping(Integer.class.getName(), "numbersChannel");
		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		Message<?> message = new GenericMessage<>("bar");
		router.handleMessage(message);
		Message<?> replyMessage = stringsChannel.receive(10000);
		assertThat(replyMessage).isNotNull();
		assertThat(replyMessage).isEqualTo(message);

		message = new GenericMessage<>(11);
		router.handleMessage(message);
		replyMessage = numbersChannel.receive(10000);
		assertThat(replyMessage).isNotNull();
		assertThat(replyMessage).isEqualTo(message);
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
			List<String> results = new ArrayList<>();
			if (name.equals("foo") || name.equals("bar")) {
				results.add("foo-channel");
				results.add("bar-channel");
			}
			return results;
		}

		public List<String> routeMessage(Message<?> message) {
			List<String> results = new ArrayList<>();
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

		private final DestinationResolver<MessageChannel> channelResolver;

		public SingleChannelInstanceRoutingTestBean(DestinationResolver<MessageChannel> channelResolver) {
			this.channelResolver = channelResolver;
		}

		public MessageChannel routePayload(String name) {
			return channelResolver.resolveDestination(name + "-channel");
		}

		public MessageChannel routeMessage(Message<?> message) {
			if (message.getPayload().equals("foo")) {
				return channelResolver.resolveDestination("foo-channel");
			}
			else if (message.getPayload().equals("bar")) {
				return channelResolver.resolveDestination("bar-channel");
			}
			return null;
		}

	}

	public static class MultiChannelInstanceRoutingTestBean {

		private final DestinationResolver<MessageChannel> channelResolver;

		public MultiChannelInstanceRoutingTestBean(DestinationResolver<MessageChannel> channelResolver) {
			this.channelResolver = channelResolver;
		}

		public List<MessageChannel> routePayload(String name) {
			List<MessageChannel> results = new ArrayList<>();
			if (name.equals("foo") || name.equals("bar")) {
				results.add(channelResolver.resolveDestination("foo-channel"));
				results.add(channelResolver.resolveDestination("bar-channel"));
			}
			return results;
		}

		public List<MessageChannel> routeMessage(Message<?> message) {
			List<MessageChannel> results = new ArrayList<>();
			if (message.getPayload().equals("foo") || message.getPayload().equals("bar")) {
				results.add(channelResolver.resolveDestination("foo-channel"));
				results.add(channelResolver.resolveDestination("bar-channel"));
			}
			return results;
		}

		public MessageChannel[] routeMessageToArray(Message<?> message) {
			MessageChannel[] results = null;
			if (message.getPayload().equals("foo") || message.getPayload().equals("bar")) {
				results = new MessageChannel[2];
				results[0] = channelResolver.resolveDestination("foo-channel");
				results[1] = channelResolver.resolveDestination("bar-channel");
			}
			return results;
		}

	}

	private static class ClassAsKeyTestBean {

		ClassAsKeyTestBean() {
			super();
		}

		@SuppressWarnings("unused")
		public Class<?> routePayload(Object payload) {
			return payload.getClass();
		}

	}

}
