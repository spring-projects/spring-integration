/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.aggregator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagingTemplate;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageGroup;

@RunWith(MockitoJUnitRunner.class)
public class MethodInvokingMessageGroupProcessorTests {

	@Mock
	private MessageChannel outputChannel;

	private List<Message<?>> messagesUpForProcessing = new ArrayList<Message<?>>(3);

	@Mock
	private MessageGroup messageGroupMock;

	@Mock
	private MessagingTemplate messagingTemplate;

	@Before
	public void initializeMessagesUpForProcessing() {
		messagesUpForProcessing.add(MessageBuilder.withPayload(1).build());
		messagesUpForProcessing.add(MessageBuilder.withPayload(2).build());
		messagesUpForProcessing.add(MessageBuilder.withPayload(4).build());
	}

	@SuppressWarnings("unused")
	private class AnnotatedAggregatorMethod {

		@Aggregator
		public Integer and(List<Integer> flags) {
			int result = 0;
			for (Integer flag : flags) {
				result = result | flag;
			}
			return result;
		}

		public String know(List<Integer> flags) {
			return "I'm not the one ";
		}
	}

	@Test
	public void shouldFindAnnotatedAggregatorMethod() throws Exception {
		MessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(new AnnotatedAggregatorMethod());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		when(outputChannel.send(isA(Message.class))).thenReturn(true);
		when(messageGroupMock.getUnmarked()).thenReturn(messagesUpForProcessing);
		processor.processAndSend(messageGroupMock, messagingTemplate, outputChannel);
		// verify
		verify(messagingTemplate).send(eq(outputChannel), messageCaptor.capture());
		assertThat((Integer) messageCaptor.getValue().getPayload(), is(7));
	}

	@SuppressWarnings("unused")
	private class SimpleAggregator {
		public Integer and(List<Integer> flags) {
			int result = 0;
			for (Integer flag : flags) {
				result = result | flag;
			}
			return result;
		}
	}

	@Test
	public void shouldFindSimpleAggregatorMethod() throws Exception {
		MessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(new SimpleAggregator());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		when(outputChannel.send(isA(Message.class))).thenReturn(true);
		when(messageGroupMock.getUnmarked()).thenReturn(messagesUpForProcessing);
		processor.processAndSend(messageGroupMock, messagingTemplate, outputChannel);
		// verify
		verify(messagingTemplate).send(eq(outputChannel), messageCaptor.capture());
		assertThat((Integer) messageCaptor.getValue().getPayload(), is(7));
	}

	@SuppressWarnings("unused")
	private class UnnanotatedAggregator {
		public Integer and(List<Integer> flags) {
			int result = 0;
			for (Integer flag : flags) {
				result = result | flag;
			}
			return result;
		}

		public void voidMethodShouldBeIgnored(List<Integer> flags) {
			fail("this method should not be invoked");
		}

		public String methodAcceptingNoCollectionShouldBeIgnored(@Header String irrelevant) {
			fail("this method should not be invoked");
			return null;
		}
	}

	@Test
	public void shouldFindFittingMethodAmongMultipleUnannotated() {
		MessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(new UnnanotatedAggregator());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

		when(outputChannel.send(isA(Message.class))).thenReturn(true);
		when(messageGroupMock.getUnmarked()).thenReturn(messagesUpForProcessing);
		processor.processAndSend(messageGroupMock, messagingTemplate, outputChannel);
		// verify
		verify(messagingTemplate).send(eq(outputChannel), messageCaptor.capture());
		assertThat((Integer) messageCaptor.getValue().getPayload(), is(7));
	}

	@SuppressWarnings("unused")
	private class AnnotatedParametersAggregator {
		public Integer and(List<Integer> flags) {
			int result = 0;
			for (Integer flag : flags) {
				result = result | flag;
			}
			return result;
		}

		public String listHeaderShouldBeIgnored(@Header List<Integer> flags) {
			fail("this method should not be invoked");
			return "";
		}
	}

	@Test
	public void shouldFindFittingMethodAmongMultipleWithAnnotatedParameters() {
		MessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(new AnnotatedParametersAggregator());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

		when(outputChannel.send(isA(Message.class))).thenReturn(true);
		when(messageGroupMock.getUnmarked()).thenReturn(messagesUpForProcessing);
		processor.processAndSend(messageGroupMock, messagingTemplate, outputChannel);
		// verify
		verify(messagingTemplate).send(eq(outputChannel), messageCaptor.capture());
		assertThat((Integer) messageCaptor.getValue().getPayload(), is(7));
	}

	@Test
	public void singleAnnotation() throws Exception {
		SingleAnnotationTestBean bean = new SingleAnnotationTestBean();
		MethodInvokingMessageGroupProcessor aggregator = new MethodInvokingMessageGroupProcessor(bean);
		Method method = this.getMethod(aggregator);
		Method expected = SingleAnnotationTestBean.class.getMethod("method1", new Class[] { List.class });
		assertEquals(expected, method);
	}

	@Test(expected = IllegalArgumentException.class)
	public void multipleAnnotations() {
		MultipleAnnotationTestBean bean = new MultipleAnnotationTestBean();
		new MethodInvokingMessageGroupProcessor(bean);
	}

	@Test
	public void noAnnotations() throws Exception {
		NoAnnotationTestBean bean = new NoAnnotationTestBean();
		MethodInvokingMessageGroupProcessor aggregator = new MethodInvokingMessageGroupProcessor(bean);
		Method method = this.getMethod(aggregator);
		Method expected = NoAnnotationTestBean.class.getMethod("method1", new Class[] { List.class });
		assertEquals(expected, method);
	}

	@Test(expected = IllegalArgumentException.class)
	public void multiplePublicMethods() {
		MultiplePublicMethodTestBean bean = new MultiplePublicMethodTestBean();
		new MethodInvokingMessageGroupProcessor(bean);
	}

	@Test(expected = IllegalArgumentException.class)
	public void noPublicMethods() {
		NoPublicMethodTestBean bean = new NoPublicMethodTestBean();
		new MethodInvokingMessageGroupProcessor(bean);
	}

	@Test
	public void jdkProxy() {
		DirectChannel input = new DirectChannel();
		QueueChannel output = new QueueChannel();
		GreetingService testBean = new GreetingBean();
		ProxyFactory proxyFactory = new ProxyFactory(testBean);
		proxyFactory.setProxyTargetClass(false);
		testBean = (GreetingService) proxyFactory.getProxy();
		MethodInvokingMessageGroupProcessor aggregator = new MethodInvokingMessageGroupProcessor(testBean);
		CorrelatingMessageHandler handler = new CorrelatingMessageHandler(aggregator);
		handler.setReleaseStrategy(new MessageCountReleaseStrategy());
		handler.setOutputChannel(output);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(input, handler);
		endpoint.start();
		Message<?> message = MessageBuilder.withPayload("proxy").setCorrelationId("abc").build();
		input.send(message);
		assertEquals("hello proxy", output.receive(0).getPayload());
	}

	@Test
	public void cglibProxy() {
		DirectChannel input = new DirectChannel();
		QueueChannel output = new QueueChannel();
		GreetingService testBean = new GreetingBean();
		ProxyFactory proxyFactory = new ProxyFactory(testBean);
		proxyFactory.setProxyTargetClass(true);
		testBean = (GreetingService) proxyFactory.getProxy();
		MethodInvokingMessageGroupProcessor aggregator = new MethodInvokingMessageGroupProcessor(testBean);
		CorrelatingMessageHandler handler = new CorrelatingMessageHandler(aggregator);
		handler.setReleaseStrategy(new MessageCountReleaseStrategy());
		handler.setOutputChannel(output);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(input, handler);
		endpoint.start();
		Message<?> message = MessageBuilder.withPayload("proxy").setCorrelationId("abc").build();
		input.send(message);
		assertEquals("hello proxy", output.receive(0).getPayload());
	}

	private Method getMethod(MethodInvokingMessageGroupProcessor aggregator) {
		Object invoker = new DirectFieldAccessor(aggregator).getPropertyValue("adapter");
		return (Method) new DirectFieldAccessor(invoker).getPropertyValue("method");
	}

	@SuppressWarnings("unused")
	private static class SingleAnnotationTestBean {

		@Aggregator
		public String method1(List<String> input) {
			return input.get(0);
		}

		public String method2(List<String> input) {
			return input.get(0);
		}
	}

	@SuppressWarnings("unused")
	private static class MultipleAnnotationTestBean {

		@Aggregator
		public String method1(List<String> input) {
			return input.get(0);
		}

		@Aggregator
		public String method2(List<String> input) {
			return input.get(0);
		}
	}

	@SuppressWarnings("unused")
	private static class NoAnnotationTestBean {

		public String method1(List<String> input) {
			return input.get(0);
		}

		String method2(List<String> input) {
			return input.get(0);
		}
	}

	@SuppressWarnings("unused")
	private static class MultiplePublicMethodTestBean {

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}

	@SuppressWarnings("unused")
	private static class NoPublicMethodTestBean {

		String lowerCase(String s) {
			return s.toLowerCase();
		}
	}

	public interface GreetingService {

		String sayHello(List<String> names);

	}

	public static class GreetingBean implements GreetingService {

		private String greeting = "hello";

		public void setGreeting(String greeting) {
			this.greeting = greeting;
		}

		@Aggregator
		public String sayHello(List<String> names) {
			return greeting + " " + names.get(0);
		}

	}

}
