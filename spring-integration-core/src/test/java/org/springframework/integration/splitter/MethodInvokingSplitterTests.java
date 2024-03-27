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

package org.springframework.integration.splitter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public class MethodInvokingSplitterTests {

	private final SplitterTestBean testBean = new SplitterTestBean();

	@Test
	public void splitStringToStringArray() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("stringToStringArray");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitStringToStringList() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("stringToStringList");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitMessageToStringArray() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("messageToStringArray");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitMessageToStringList() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("messageToStringList");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitMessageToMessageArray() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("messageToMessageArray");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitMessageToMessageList() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("messageToMessageList");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitMessageToMessageBuilderList() {
		Message<String> message = MessageBuilder.withPayload("foo.bar")
				.setHeader("myHeader", "myValue")
				.build();
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "messageToMessageBuilderList");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.afterPropertiesSet();
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		assertThat(reply1.getHeaders().get("myHeader")).isEqualTo("myValue");
		assertThat(reply1.getHeaders().get("foo")).isEqualTo("bar");

		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
		assertThat(reply2.getHeaders().get("myHeader")).isEqualTo("myValue");
		assertThat(reply2.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void splitStringToMessageArray() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("stringToMessageArray");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitStringToMessageList() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("stringToMessageList");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitStringToStringArrayConfiguredByMethodName() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("stringToStringArray");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitStringToStringListConfiguredByMethodName() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("stringToStringList");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitMessageToStringArrayConfiguredByMethodName() {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "messageToStringArray");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.afterPropertiesSet();
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitMessageToStringListConfiguredByMethodName() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("messageToStringList");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitMessageToMessageArrayConfiguredByMethodName() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("messageToMessageArray");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitMessageToMessageListConfiguredByMethodName() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("messageToMessageList");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitStringToMessageArrayConfiguredByMethodName() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = getSplitter("stringToMessageArray");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitStringToMessageListConfiguredByMethodName() {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "stringToMessageList");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.afterPropertiesSet();
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitListPayload() {
		class ListSplitter {

			@SuppressWarnings("unused")
			public List<String> split(List<String> list) {
				return list;
			}

		}

		GenericMessage<List<?>> message = new GenericMessage<>(Arrays.asList("foo", "bar"));
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new ListSplitter(), "split");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.afterPropertiesSet();
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void splitStreamPayload() {
		class StreamSplitter {

			@SuppressWarnings("unused")
			public Stream<String> split(Stream<String> stream) {
				return stream;
			}

		}

		Stream<String> stream = Stream.of("foo", "bar");
		ProxyFactory pf = new ProxyFactory(stream);
		AtomicBoolean closed = new AtomicBoolean();
		MethodInterceptor interceptor = i -> {
			if (i.getMethod().getName().equals("close")) {
				closed.set(true);
			}
			return i.proceed();
		};
		pf.addAdvice(interceptor);
		stream = (Stream<String>) pf.getProxy();
		GenericMessage<Stream<?>> message = new GenericMessage<>(stream);
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new StreamSplitter(), "split");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.afterPropertiesSet();
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
		assertThat(closed.get()).as("Expected stream.close()").isTrue();
	}

	@Test
	public void headerForObjectReturnValues() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("stringToStringArray");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getSequenceSize()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber()).isEqualTo(1);
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getSequenceSize()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
	}

	@Test
	public void headerForMessageReturnValues() throws Exception {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("messageToMessageList");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getSequenceSize()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber()).isEqualTo(1);
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getSequenceSize()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
	}

	@Test
	public void splitMessageHeader() throws Exception {
		Message<String> message = MessageBuilder.withPayload("ignored").setHeader("testHeader", "foo.bar").build();
		MethodInvokingSplitter splitter = this.getSplitter("splitHeader");
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void splitPayloadAndHeader() throws Exception {
		Message<String> message = MessageBuilder.withPayload("a.b").setHeader("testHeader", "c.d").build();
		Method splittingMethod = this.testBean.getClass()
				.getMethod("splitPayloadAndHeader", String.class, String.class);
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, splittingMethod);
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.afterPropertiesSet();
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("a");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("b");
		Message<?> reply3 = replies.get(2);
		assertThat(reply3).isNotNull();
		assertThat(reply3.getPayload()).isEqualTo("c");
		Message<?> reply4 = replies.get(3);
		assertThat(reply4).isNotNull();
		assertThat(reply4.getPayload()).isEqualTo("d");
	}

	@Test
	public void singleAnnotation() {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		SingleAnnotationTestBean annotatedBean = new SingleAnnotationTestBean();
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(annotatedBean);
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.afterPropertiesSet();
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void ambiguousTypeMatch() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MethodInvokingSplitter(new AmbiguousTypeMatchTestBean()));
	}

	@Test
	public void singlePublicMethod() {
		GenericMessage<String> message = new GenericMessage<>("foo.bar");
		SinglePublicMethodTestBean testBean = new SinglePublicMethodTestBean();
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean);
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.afterPropertiesSet();
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foo");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void multiplePublicMethods() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MethodInvokingSplitter(new MultiplePublicMethodTestBean()));
	}

	private MethodInvokingSplitter getSplitter(String methodName) throws Exception {
		Class<?> paramType = methodName.startsWith("message") ? Message.class : String.class;
		Method splittingMethod = this.testBean.getClass().getMethod(methodName, paramType);
		MethodInvokingSplitter methodInvokingSplitter = new MethodInvokingSplitter(this.testBean, splittingMethod);
		methodInvokingSplitter.setBeanFactory(mock(BeanFactory.class));
		methodInvokingSplitter.afterPropertiesSet();
		return methodInvokingSplitter;
	}

	public static class SplitterTestBean {

		public String[] stringToStringArray(String input) {
			return input.split("\\.");
		}

		public List<String> stringToStringList(String input) {
			return Arrays.asList(input.split("\\."));
		}

		public String[] messageToStringArray(Message<?> input) {
			return input.getPayload().toString().split("\\.");
		}

		public List<String> messageToStringList(Message<?> input) {
			return Arrays.asList(input.getPayload().toString().split("\\."));
		}

		public Message<String>[] messageToMessageArray(Message<?> input) {
			String[] strings = input.getPayload().toString().split("\\.");
			Message<String>[] messages = new TestStringMessage[strings.length];
			for (int i = 0; i < strings.length; i++) {
				messages[i] = new TestStringMessage(strings[i]);
			}
			return messages;
		}

		public List<Message<String>> messageToMessageList(Message<?> input) {
			String[] strings = input.getPayload().toString().split("\\.");
			List<Message<String>> messages = new ArrayList<>();
			for (String s : strings) {
				messages.add(new GenericMessage<String>(s));
			}
			return messages;
		}

		public List<AbstractIntegrationMessageBuilder<String>> messageToMessageBuilderList(Message<?> input) {
			String[] strings = input.getPayload().toString().split("\\.");
			List<AbstractIntegrationMessageBuilder<String>> messageBuilders = new ArrayList<>();
			for (String s : strings) {
				MessageBuilder<String> builder = MessageBuilder.withPayload(s)
						.setHeader("foo", "bar");
				messageBuilders.add(builder);
			}
			return messageBuilders;
		}

		public Message<String>[] stringToMessageArray(String input) {
			String[] strings = input.split("\\.");
			Message<String>[] messages = new TestStringMessage[strings.length];
			for (int i = 0; i < strings.length; i++) {
				messages[i] = new TestStringMessage(strings[i]);
			}
			return messages;
		}

		public List<Message<String>> stringToMessageList(String input) {
			String[] strings = input.split("\\.");
			List<Message<String>> messages = new ArrayList<>();
			for (String s : strings) {
				messages.add(new GenericMessage<>(s));
			}
			return messages;
		}

		public String[] splitHeader(@Header("testHeader") String input) {
			return input.split("\\.");
		}

		public List<String> splitPayloadAndHeader(String payload, @Header("testHeader") String header) {
			String regex = "\\.";
			List<String> results = new ArrayList<>();
			Collections.addAll(results, payload.split(regex));
			Collections.addAll(results, header.split(regex));
			return results;
		}

	}

	public static class SingleAnnotationTestBean {

		@Splitter
		public String[] annotatedMethod(String input) {
			return input.split("\\.");
		}

		public String[] anotherMethod(String input) {
			throw new UnsupportedOperationException("incorrect test invocation");
		}

	}

	public static class AmbiguousTypeMatchTestBean {

		@Splitter
		public String[] method1(String input) {
			throw new UnsupportedOperationException("incorrect test invocation");
		}

		@Splitter
		public String[] method2(String input) {
			throw new UnsupportedOperationException("incorrect test invocation");
		}

	}

	public static class SinglePublicMethodTestBean {

		public String[] publicMethod(String input) {
			return input.split("\\.");
		}

		String[] anotherMethod(String input) {
			throw new UnsupportedOperationException("incorrect test invocation");
		}

	}

	public static class MultiplePublicMethodTestBean {

		public String[] method1(String input) {
			throw new UnsupportedOperationException("incorrect test invocation");
		}

		public String[] method2(String input) {
			throw new UnsupportedOperationException("incorrect test invocation");
		}

	}

	@SuppressWarnings("serial")
	private static class TestStringMessage extends GenericMessage<String> {

		TestStringMessage(String payload) {
			super(payload);
		}

	}

}
