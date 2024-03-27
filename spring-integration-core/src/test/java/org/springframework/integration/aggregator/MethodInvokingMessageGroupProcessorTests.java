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

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.Payloads;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 */
@RunWith(MockitoJUnitRunner.class)
public class MethodInvokingMessageGroupProcessorTests {

	private final List<Message<?>> messagesUpForProcessing = new ArrayList<>(3);

	@Mock
	private MessageGroup messageGroupMock;

	@Before
	public void initializeMessagesUpForProcessing() {
		this.messagesUpForProcessing.add(MessageBuilder.withPayload(1).build());
		this.messagesUpForProcessing.add(MessageBuilder.withPayload(2).build());
		this.messagesUpForProcessing.add(MessageBuilder.withPayload(4).build());
	}

	@Test
	public void shouldFindAnnotatedAggregatorMethod() {

		@SuppressWarnings("unused")
		class AnnotatedAggregatorMethod {

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

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new AnnotatedAggregatorMethod());
		processor.setBeanFactory(mock(BeanFactory.class));
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload()).isEqualTo(7);
	}

	@Test
	public void shouldFindSimpleAggregatorMethod() {

		@SuppressWarnings("unused")
		class SimpleAggregator {

			public Integer and(List<Integer> flags) {
				int result = 0;
				for (Integer flag : flags) {
					result = result | flag;
				}
				return result;
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new SimpleAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload()).isEqualTo(7);
	}

	@Test
	public void shouldFindSimpleAggregatorMethodForMessages() {

		@SuppressWarnings("unused")
		class SimpleAggregator {

			public Integer and(List<Message<Integer>> flags) {
				int result = 0;
				for (Message<Integer> flag : flags) {
					result = result | flag.getPayload();
				}
				return result;
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new SimpleAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload()).isEqualTo(7);
	}

	@Test
	public void shouldFindListPayloads() {

		@SuppressWarnings("unused")
		class SimpleAggregator {

			public String and(List<Integer> flags, @Header("foo") List<Integer> header) {
				List<Integer> result = new ArrayList<Integer>();
				for (int flag : flags) {
					result.add(flag);
				}
				for (int flag : header) {
					result.add(flag);
				}
				return result.toString();
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new SimpleAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		messagesUpForProcessing.add(MessageBuilder.withPayload(3).setHeader("foo", Arrays.asList(101, 102)).build());
		when(messageGroupMock.getMessages()).thenReturn(messagesUpForProcessing);
		Object result = processor.processMessageGroup(messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload())
				.isEqualTo("[1, 2, 4, 3, 101, 102]");
	}

	@Test
	public void shouldFindAnnotatedPayloadsWithNoType() {

		@SuppressWarnings("unused")
		class SimpleAggregator {

			public String and(@Payloads List<?> rawFlags, @Header("foo") List<Integer> header) {
				@SuppressWarnings("unchecked")
				List<Integer> flags = (List<Integer>) rawFlags;
				List<Integer> result = new ArrayList<Integer>();
				for (int flag : flags) {
					result.add(flag);
				}
				for (int flag : header) {
					result.add(flag);
				}
				return result.toString();
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new SimpleAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		this.messagesUpForProcessing.add(
				MessageBuilder.withPayload(3)
						.setHeader("foo", Arrays.asList(101, 102))
						.build());
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload())
				.isEqualTo("[1, 2, 4, 3, 101, 102]");
	}

	@Test
	public void shouldUseAnnotatedPayloads() {

		@SuppressWarnings("unused")
		class SimpleAggregator {

			@Aggregator
			public String and(@Payloads List<Integer> flags) {
				return flags.toString();
			}

			public String or(List<Integer> flags) {
				throw new UnsupportedOperationException("Not expected");
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new SimpleAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload()).isEqualTo("[1, 2, 4]");
	}

	@Test
	public void shouldFindSimpleAggregatorMethodWithCollection() {

		@SuppressWarnings("unused")
		class SimpleAggregator {

			public Integer and(Collection<Integer> flags) {
				int result = 0;
				for (Integer flag : flags) {
					result = result | flag;
				}
				return result;
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new SimpleAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload()).isEqualTo(7);
	}

	@Test
	public void shouldFindSimpleAggregatorMethodWithArray() {

		@SuppressWarnings("unused")
		class SimpleAggregator {

			public Integer and(int[] flags) {
				int result = 0;
				for (int flag : flags) {
					result = result | flag;
				}
				return result;
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new SimpleAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload()).isEqualTo(7);
	}

	@Test
	public void shouldFindSimpleAggregatorMethodWithIterator() {

		@SuppressWarnings("unused")
		class SimpleAggregator {

			public Integer and(Iterator<Integer> flags) {
				int result = 0;
				while (flags.hasNext()) {
					result = result | flags.next();
				}
				return result;
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new SimpleAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		GenericConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<ArrayList<?>, Iterator<?>>() { // Must not be lambda

			@Override
			public Iterator<?> convert(ArrayList<?> source) {
				return source.iterator();
			}

		});
		processor.setConversionService(conversionService);
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload()).isEqualTo(7);
	}

	@Test
	public void shouldFindFittingMethodAmongMultipleUnannotated() {

		@SuppressWarnings("unused")
		class UnannotatedAggregator {

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

			public String methodAcceptingNoCollectionShouldBeIgnored(String irrelevant) {
				fail("this method should not be invoked");
				return null;
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new UnannotatedAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload()).isEqualTo(7);
	}

	@Test
	public void shouldFindFittingMethodForIteratorOfMessages() {

		@SuppressWarnings("unused")
		class UnannotatedAggregator {

			public Iterator<?> and(Iterator<Message<?>> flags) {

				return flags;
			}

			public void voidMethodShouldBeIgnored(List<Integer> flags) {
				fail("this method should not be invoked");
			}

			public String methodAcceptingNoCollectionShouldBeIgnored(String irrelevant) {
				fail("this method should not be invoked");
				return null;
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new UnannotatedAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		assertThat(((AbstractIntegrationMessageBuilder<?>) result).build().getPayload()).isInstanceOf(Iterator.class);
	}

	@Test
	public void testTwoMethodsWithSameParameterTypesAmbiguous() {

		class AnnotatedParametersAggregator {

			@SuppressWarnings("unused")
			public Integer and(List<Integer> flags) {
				int result = 0;
				for (Integer flag : flags) {
					result = result | flag;
				}
				return result;
			}

			@SuppressWarnings("unused")
			public String listHeaderShouldBeIgnored(@Header List<Integer> flags) {
				fail("this method should not be invoked");
				return "";
			}

		}

		MethodInvokingMessageGroupProcessor processor =
				new MethodInvokingMessageGroupProcessor(new AnnotatedParametersAggregator());
		processor.setBeanFactory(mock(BeanFactory.class));
		when(this.messageGroupMock.getMessages()).thenReturn(this.messagesUpForProcessing);
		Object result = processor.processMessageGroup(this.messageGroupMock);
		Object payload = ((AbstractIntegrationMessageBuilder<?>) result).build().getPayload();
		assertThat(payload instanceof Integer).isTrue();
		assertThat(payload).isEqualTo(7);

	}

	@Test
	public void singleAnnotation() {

		@SuppressWarnings("unused")
		class SingleAnnotationTestBean {

			@Aggregator
			public String method1(List<String> input) {
				return input.get(0);
			}

			public String method2(List<String> input) {
				return input.get(1);
			}

		}

		SingleAnnotationTestBean bean = new SingleAnnotationTestBean();
		MethodInvokingMessageGroupProcessor aggregator = new MethodInvokingMessageGroupProcessor(bean);
		aggregator.setBeanFactory(mock(BeanFactory.class));
		SimpleMessageGroup group = new SimpleMessageGroup("FOO");
		group.add(new GenericMessage<>("foo"));
		group.add(new GenericMessage<>("bar"));
		assertThat(aggregator.aggregatePayloads(group, null)).isEqualTo("foo");
	}

	@Test
	public void testHeaderParameters() {

		class SingleAnnotationTestBean {

			@Aggregator
			public String method1(List<String> input, @Header("foo") String foo) {
				return input.get(0) + foo;
			}

		}

		SingleAnnotationTestBean bean = new SingleAnnotationTestBean();
		MethodInvokingMessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(bean);
		processor.setBeanFactory(mock(BeanFactory.class));
		SimpleMessageGroup group = new SimpleMessageGroup("FOO");
		group.add(MessageBuilder.withPayload("foo").setHeader("foo", "bar").build());
		group.add(MessageBuilder.withPayload("bar").setHeader("foo", "bar").build());
		assertThat(processor.aggregatePayloads(group, processor.aggregateHeaders(group))).isEqualTo("foobar");
	}

	@Test
	public void testHeadersParameters() {

		class SingleAnnotationTestBean {

			@Aggregator
			public String method1(List<String> input, @Headers Map<String, ?> map) {
				return input.get(0) + map.get("foo");
			}

		}

		SingleAnnotationTestBean bean = new SingleAnnotationTestBean();
		MethodInvokingMessageGroupProcessor aggregator = new MethodInvokingMessageGroupProcessor(bean);
		aggregator.setBeanFactory(mock(BeanFactory.class));
		SimpleMessageGroup group = new SimpleMessageGroup("FOO");
		group.add(MessageBuilder.withPayload("foo").setHeader("foo", "bar").build());
		group.add(MessageBuilder.withPayload("bar").setHeader("foo", "bar").build());
		assertThat(aggregator.aggregatePayloads(group, aggregator.aggregateHeaders(group))).isEqualTo("foobar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void multipleAnnotations() {

		class MultipleAnnotationTestBean {

			@Aggregator
			public String method1(List<String> input) {
				return input.get(0);
			}

			@Aggregator
			public String method2(List<String> input) {
				return input.get(0);
			}

		}

		MultipleAnnotationTestBean bean = new MultipleAnnotationTestBean();
		new MethodInvokingMessageGroupProcessor(bean);
	}

	@Test
	public void noAnnotations() {

		@SuppressWarnings("unused")
		class NoAnnotationTestBean {

			public String method1(List<String> input) {
				return input.get(0);
			}

			String method2(List<String> input) {
				return input.get(1);
			}

		}

		NoAnnotationTestBean bean = new NoAnnotationTestBean();
		MethodInvokingMessageGroupProcessor aggregator = new MethodInvokingMessageGroupProcessor(bean);
		aggregator.setBeanFactory(mock(BeanFactory.class));
		SimpleMessageGroup group = new SimpleMessageGroup("FOO");
		group.add(new GenericMessage<>("foo"));
		group.add(new GenericMessage<>("bar"));
		assertThat(aggregator.aggregatePayloads(group, null)).isEqualTo("foo");
	}

	@Test(expected = IllegalArgumentException.class)
	public void multiplePublicMethods() {

		@SuppressWarnings("unused")
		class MultiplePublicMethodTestBean {

			public String upperCase(String s) {
				return s.toUpperCase();
			}

			public String lowerCase(String s) {
				return s.toLowerCase();
			}

		}

		MultiplePublicMethodTestBean bean = new MultiplePublicMethodTestBean();
		new MethodInvokingMessageGroupProcessor(bean);
	}

	@Test(expected = IllegalStateException.class)
	public void noPublicMethods() {

		@SuppressWarnings("unused")
		class NoPublicMethodTestBean {

			String lowerCase(String s) {
				return s.toLowerCase();
			}

		}

		NoPublicMethodTestBean bean = new NoPublicMethodTestBean();
		new MethodInvokingMessageGroupProcessor(bean);
	}

	@Test
	public void jdkProxy() {
		DirectChannel input = new DirectChannel();
		QueueChannel output = new QueueChannel();
		GreetingService testBean = new GreetingBean();
		ProxyFactory proxyFactory = new ProxyFactory(testBean);
		proxyFactory.setProxyTargetClass(true);
		testBean = (GreetingService) proxyFactory.getProxy();
		MethodInvokingMessageGroupProcessor aggregator = new MethodInvokingMessageGroupProcessor(testBean);
		AggregatingMessageHandler handler = new AggregatingMessageHandler(aggregator);
		handler.setReleaseStrategy(new MessageCountReleaseStrategy());
		handler.setOutputChannel(output);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		EventDrivenConsumer endpoint = new EventDrivenConsumer(input, handler);
		endpoint.start();

		Message<?> message = MessageBuilder.withPayload("proxy").setCorrelationId("abc").build();
		input.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo("hello proxy");
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
		AggregatingMessageHandler handler = new AggregatingMessageHandler(aggregator);
		handler.setReleaseStrategy(new MessageCountReleaseStrategy());
		handler.setOutputChannel(output);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		EventDrivenConsumer endpoint = new EventDrivenConsumer(input, handler);
		endpoint.start();

		Message<?> message = MessageBuilder.withPayload("proxy").setCorrelationId("abc").build();
		input.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo("hello proxy");
	}

	public interface GreetingService {

		String sayHello(List<String> names);

	}

	public static class GreetingBean implements GreetingService {

		private String greeting = "hello";

		public void setGreeting(String greeting) {
			this.greeting = greeting;
		}

		@Override
		@Aggregator
		public String sayHello(List<String> names) {
			return greeting + " " + names.get(0);
		}

	}

}
