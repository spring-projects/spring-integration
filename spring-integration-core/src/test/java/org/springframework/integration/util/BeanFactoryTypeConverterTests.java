/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
public class BeanFactoryTypeConverterTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testEmptyCollectionConversion() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		List<String> sourceObject = new ArrayList<>();
		ArrayList<BeanFactoryTypeConverterTests> convertedCollection =
				(ArrayList<BeanFactoryTypeConverterTests>) typeConverter.convertValue(sourceObject,
						TypeDescriptor.forObject(sourceObject),
						TypeDescriptor.forObject(new ArrayList<BeanFactoryTypeConverterTests>()));
		assertThat(convertedCollection).isEqualTo(sourceObject);
	}

	@Test
	public void testToStringConversion() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		String converted = (String) typeConverter.convertValue(1234, TypeDescriptor.valueOf(Integer.class),
				TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("1234");
	}

	@Test
	public void testToNonStringConversionNotSupportedByGenericConversionService() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		@SuppressWarnings("unchecked")
		Collection<Integer> converted = (Collection<Integer>) typeConverter.convertValue(1234,
				TypeDescriptor.valueOf(Integer.class),
				TypeDescriptor.forObject(new ArrayList<>(Arrays.asList(1))));
		assertThat(converted).isEqualTo(Collections.singletonList(1234));
	}

	@Test
	public void testMessageHeadersNotConverted() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		MessageHeaders headers = new GenericMessage<>("foo").getHeaders();
		assertThat(typeConverter.convertValue(headers, TypeDescriptor.valueOf(MessageHeaders.class),
				TypeDescriptor.valueOf(MessageHeaders.class))).isSameAs(headers);
	}

	@Test
	public void testMessageHistoryNotConverted() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		Message<String> message = new GenericMessage<>("foo");
		message = MessageHistory.write(message, new NamedComponent() {

			@Override
			public String getComponentName() {
				return "bar";
			}

			@Override
			public String getComponentType() {
				return "baz";
			}
		});
		MessageHistory history = MessageHistory.read(message);
		assertThat(typeConverter.convertValue(history, TypeDescriptor.valueOf(MessageHistory.class),
				TypeDescriptor.valueOf(MessageHistory.class))).isSameAs(history);
	}

	@Test
	public void testByteArrayNotConverted() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		byte[] bytes = new byte[1];
		assertThat(typeConverter.convertValue(bytes, TypeDescriptor.valueOf(byte[].class),
				TypeDescriptor.valueOf(byte[].class))).isSameAs(bytes);
	}

	@Test
	public void testStringToObjectNotConverted() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		String string = "foo";
		assertThat(typeConverter.convertValue(string, TypeDescriptor.valueOf(String.class),
				TypeDescriptor.valueOf(Object.class))).isSameAs(string);
	}

	@Test
	public void testObjectToStringIsConverted() {
		ConversionService conversionService = mock(ConversionService.class);
		when(conversionService.canConvert(any(TypeDescriptor.class), any(TypeDescriptor.class)))
				.thenReturn(true);
		when(conversionService.convert(any(), any(TypeDescriptor.class), any(TypeDescriptor.class)))
				.thenReturn("foo");
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		Object object = new Object();
		assertThat(typeConverter.convertValue(object, TypeDescriptor.valueOf(Object.class),
				TypeDescriptor.valueOf(String.class))).isEqualTo("foo");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMapOfMapOfCollectionIsConverted() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<Foo, Bar>() { // Must be explicit type with generics

			@Override
			public Bar convert(Foo source) {
				return new Bar();
			}
		});
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		beanFactory.setConversionService(conversionService);
		typeConverter.setBeanFactory(beanFactory);
		Map<String, Map<String, Set<Foo>>> foos;
		Map<String, Map<String, Set<Bar>>> bars;

		TypeDescriptor sourceType = TypeDescriptor.map(Map.class, null, null);
		TypeDescriptor targetType = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class),
				TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class),
						TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(Bar.class))));

		Set<Foo> fooSet = new HashSet<>();
		fooSet.add(new Foo());
		Map<String, Set<Foo>> fooMap = new HashMap<>();
		fooMap.put("foo", fooSet);
		foos = new HashMap<>();
		foos.put("foo", fooMap);

		bars = (Map<String, Map<String, Set<Bar>>>) typeConverter.convertValue(foos, sourceType, targetType);
		assertThat(bars.get("foo").get("foo").iterator().next()).isInstanceOf(Bar.class);

		Service service = new Service();
		MethodInvokingMessageProcessor<Service> processor = new MethodInvokingMessageProcessor<>(service, "handle");
		processor.setConversionService(conversionService);
		processor.setUseSpelInvoker(true);
		processor.setBeanFactory(beanFactory);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(processor);
		QueueChannel replyChannel = new QueueChannel();
		handler.setOutputChannel(replyChannel);
		handler.handleMessage(new GenericMessage<>(foos));
		Message<?> message = replyChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("bar");
	}

	@Test
	public void testCollectionIsConverted() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<Foo, Bar>() { // Must be explicit type with generics

			@Override
			public Bar convert(Foo source) {
				return new Bar();
			}
		});
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		beanFactory.setConversionService(conversionService);
		typeConverter.setBeanFactory(beanFactory);

		Service service = new Service();
		MethodInvokingMessageProcessor<Service> processor = new MethodInvokingMessageProcessor<>(service, "handle");
		processor.setConversionService(conversionService);
		processor.setUseSpelInvoker(true);
		processor.setBeanFactory(beanFactory);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(processor);
		QueueChannel replyChannel = new QueueChannel();
		handler.setOutputChannel(replyChannel);
		handler.handleMessage(new GenericMessage<Collection<Foo>>(Collections.singletonList(new Foo())));
		Message<?> message = replyChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("baz");
	}

	@Test
	public void testNullArg() {
		DefaultConversionService conversionService = new DefaultConversionService();
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		Object foo = typeConverter.convertValue(null, null, TypeDescriptor.valueOf(Bar.class));
		assertThat(foo).isNull();
	}

	@Test
	public void testVoidArg() {
		DefaultConversionService conversionService = new DefaultConversionService();
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		Object foo = typeConverter.convertValue(null, null, TypeDescriptor.valueOf(Void.class));
		assertThat(foo).isNull();
		foo = typeConverter.convertValue(null, null, TypeDescriptor.valueOf(Void.TYPE));
		assertThat(foo).isNull();
	}

	@Test
	public void testEditorWithTargetString() {
		DefaultConversionService conversionService = new DefaultConversionService();
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		UUID uuid = UUID.randomUUID();
		Object foo = typeConverter.convertValue(uuid, TypeDescriptor.valueOf(UUID.class),
				TypeDescriptor.valueOf(String.class));
		assertThat(foo).isEqualTo(uuid.toString());
	}

	@Test
	public void testEditorWithTargetFoo() {
		DefaultConversionService conversionService = new DefaultConversionService();
		final Foo foo = new Foo();
		conversionService.addConverter(new Converter<String, Foo>() { // Must be explicit type with generics

			@Override
			public Foo convert(String source) {
				return foo;
			}
		});
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		UUID uuid = UUID.randomUUID();
		Object convertedFoo = typeConverter.convertValue(uuid, TypeDescriptor.valueOf(UUID.class),
				TypeDescriptor.valueOf(Foo.class));
		assertThat(convertedFoo).isSameAs(foo);
	}

	@Test
	public void testDelegateWithTargetUUID() {
		DefaultConversionService conversionService = new DefaultConversionService();
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		UUID uuid = UUID.randomUUID();
		Object converted = typeConverter.convertValue(uuid.toString(), TypeDescriptor.valueOf(String.class),
				TypeDescriptor.valueOf(UUID.class));
		assertThat(converted).isEqualTo(uuid);
	}

	@Test
	@Ignore("Too sensitive for the time")
	public void initialConcurrency() throws Exception {
		ConversionService conversionService = mock(ConversionService.class); // can convert nothing so we drop down to
		// P.E.s
		final BeanFactoryTypeConverter beanFactoryTypeConverter = new BeanFactoryTypeConverter(conversionService);
		ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);
		SimpleTypeConverter typeConverter = spy(new SimpleTypeConverter());
		when(beanFactory.getTypeConverter()).thenReturn(typeConverter);
		final AtomicBoolean inGetDefaultEditor = new AtomicBoolean();
		final AtomicBoolean concurrentlyInGetDefaultEditor = new AtomicBoolean();
		final AtomicInteger count = new AtomicInteger();
		doAnswer(invocation -> {
			count.incrementAndGet();
			Thread.sleep(100);
			concurrentlyInGetDefaultEditor.set(inGetDefaultEditor.getAndSet(true));
			Thread.sleep(100);
			inGetDefaultEditor.set(false);
			return invocation.callRealMethod();
		}).when(typeConverter).getDefaultEditor(UUID.class);
		beanFactoryTypeConverter.setBeanFactory(beanFactory);
		final TypeDescriptor sourceType = TypeDescriptor.valueOf(UUID.class);
		final TypeDescriptor targetType = TypeDescriptor.valueOf(String.class);
		ExecutorService exec = Executors.newFixedThreadPool(2);
		Runnable test = () -> {
			beanFactoryTypeConverter.canConvert(sourceType, targetType);
			beanFactoryTypeConverter.convertValue(UUID.randomUUID(), sourceType, targetType);
		};
		exec.execute(test);
		exec.execute(test);
		exec.shutdown();
		assertThat(exec.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		assertThat(count.get()).isEqualTo(4);
		assertThat(concurrentlyInGetDefaultEditor.get()).isFalse();
	}

	public static class Foo {

	}

	public static class Bar {

	}

	public static class Service {

		public String handle(Map<String, Map<String, Set<Bar>>> payload) {
			assertThat(payload.get("foo").get("foo").iterator().next()).isInstanceOf(Bar.class);
			return "bar";
		}

		public String handle(Collection<Bar> payload) {
			assertThat(payload.iterator().next()).isInstanceOf(Bar.class);
			return "baz";
		}

	}

}
