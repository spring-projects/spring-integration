/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.util;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 *
 */
public class BeanFactoryTypeConverterTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testEmptyCollectionConversion(){
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		List<String> sourceObject = new ArrayList<String>();
		ArrayList<BeanFactoryTypeConverterTests> convertedCollection =
			(ArrayList<BeanFactoryTypeConverterTests>) typeConverter.convertValue(sourceObject, TypeDescriptor.forObject(sourceObject), TypeDescriptor.forObject(new ArrayList<BeanFactoryTypeConverterTests>()));
		assertEquals(sourceObject, convertedCollection);
	}

	@Test
	public void testToStringConversion(){
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		String converted = (String) typeConverter.convertValue(new Integer(1234), TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class));
		assertEquals("1234", converted);
	}

	@Test
	public void testToNonStringConversionNotSupportedByGenericConversionService(){
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		@SuppressWarnings("unchecked")
		Collection<Integer> converted = (Collection<Integer>) typeConverter.convertValue(new Integer(1234), TypeDescriptor.valueOf(Integer.class), TypeDescriptor.forObject(new ArrayList<Integer>(Arrays.asList(1))));
		assertEquals(Arrays.asList(1234), converted);
	}

	@Test
	public void testMessageHeadersNotConverted() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		MessageHeaders headers = new GenericMessage<String>("foo").getHeaders();
		assertSame(headers, typeConverter.convertValue(headers, TypeDescriptor.valueOf(MessageHeaders.class), TypeDescriptor.valueOf(MessageHeaders.class)));
	}

	@Test
	public void testMessageHistoryNotConverted() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		Message<String> message = new GenericMessage<String>("foo");
		message = MessageHistory.write(message, new NamedComponent(){
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
		assertSame(history, typeConverter.convertValue(history, TypeDescriptor.valueOf(MessageHistory.class), TypeDescriptor.valueOf(MessageHistory.class)));
	}

	@Test
	public void testByteArrayNotConverted() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		byte[] bytes = new byte[1];
		assertSame(bytes, typeConverter.convertValue(bytes, TypeDescriptor.valueOf(byte[].class), TypeDescriptor.valueOf(byte[].class)));
	}

	@Test
	public void testStringToObjectNotConverted() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		String string = "foo";
		assertSame(string, typeConverter.convertValue(string, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Object.class)));
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
		assertEquals("foo", typeConverter.convertValue(object, TypeDescriptor.valueOf(Object.class), TypeDescriptor.valueOf(String.class)));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMapOfMapOfCollectionIsConverted() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<Foo, Bar>() {
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

		Set<Foo> fooSet = new HashSet<Foo>();
		fooSet.add(new Foo());
		Map<String, Set<Foo>> fooMap = new HashMap<String, Set<Foo>>();
		fooMap.put("foo", fooSet);
		foos = new HashMap<String, Map<String, Set<Foo>>>();
		foos.put("foo", fooMap);

		bars = (Map<String, Map<String, Set<Bar>>>) typeConverter.convertValue(foos, sourceType, targetType);
		assertThat(bars.get("foo").get("foo").iterator().next(), instanceOf(Bar.class));

		Service service = new Service();
		MethodInvokingMessageProcessor<Service> processor = new	MethodInvokingMessageProcessor<Service>(service, "handle");
		processor.setConversionService(conversionService);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(processor);
		QueueChannel replyChannel = new QueueChannel();
		handler.setOutputChannel(replyChannel);
		handler.handleMessage(new GenericMessage<Map<String, Map<String, Set<Foo>>>>(foos));
		Message<?> message = replyChannel.receive(0);
		assertNotNull(message);
		assertEquals("bar", message.getPayload());
	}

	@Test
	public void testCollectionIsConverted() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<Foo, Bar>() {
			@Override
			public Bar convert(Foo source) {
				return new Bar();
			}
		});
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		beanFactory.setConversionService(conversionService);
		typeConverter.setBeanFactory(beanFactory);

		Service service = new Service();
		MethodInvokingMessageProcessor<Service> processor = new	MethodInvokingMessageProcessor<Service>(service, "handle");
		processor.setConversionService(conversionService);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(processor);
		QueueChannel replyChannel = new QueueChannel();
		handler.setOutputChannel(replyChannel);
		handler.handleMessage(new GenericMessage<Collection<Foo>>(Collections.singletonList(new Foo())));
		Message<?> message = replyChannel.receive(0);
		assertNotNull(message);
		assertEquals("baz", message.getPayload());
	}

	@Test
	public void testNullArg() {
		DefaultConversionService conversionService = new DefaultConversionService();
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		Object foo = typeConverter.convertValue(null, null, TypeDescriptor.valueOf(Bar.class));
		assertNull(foo);
	}

	@Test
	public void testVoidArg() {
		DefaultConversionService conversionService = new DefaultConversionService();
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		Object foo = typeConverter.convertValue(null, null, TypeDescriptor.valueOf(Void.class));
		assertNull(foo);
		foo = typeConverter.convertValue(null, null, TypeDescriptor.valueOf(Void.TYPE));
		assertNull(foo);
	}

	@Test
	public void testEditorWithTargetString() {
		DefaultConversionService conversionService = new DefaultConversionService();
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		UUID uuid = UUID.randomUUID();
		Object foo = typeConverter.convertValue(uuid, TypeDescriptor.valueOf(UUID.class),
				TypeDescriptor.valueOf(String.class));
		assertEquals(uuid.toString(), foo);
	}

	@Test
	public void testEditorWithTargetFoo() {
		DefaultConversionService conversionService = new DefaultConversionService();
		final Foo foo = new Foo();
		conversionService.addConverter(new Converter<String, Foo>() {
			@Override
			public Foo convert(String source) {
				return foo;
			}
		});
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		UUID uuid = UUID.randomUUID();
		Object convertedFoo = typeConverter.convertValue(uuid, TypeDescriptor.valueOf(UUID.class),
				TypeDescriptor.valueOf(Foo.class));
		assertSame(foo, convertedFoo);
	}

	@Test
	public void testDelegateWithTargetUUID() {
		DefaultConversionService conversionService = new DefaultConversionService();
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		UUID uuid = UUID.randomUUID();
		Object converted = typeConverter.convertValue(uuid.toString(), TypeDescriptor.valueOf(String.class),
				TypeDescriptor.valueOf(UUID.class));
		assertEquals(uuid, converted);
	}

	@Test
	public void initialConcurrency() throws Exception {
		ConversionService conversionService = mock(ConversionService.class); // can convert nothing so we drop down to P.E.s
		final BeanFactoryTypeConverter beanFactoryTypeConverter = new BeanFactoryTypeConverter(conversionService);
		ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);
		SimpleTypeConverter typeConverter = spy(new SimpleTypeConverter());
		when(beanFactory.getTypeConverter()).thenReturn(typeConverter);
		final AtomicBoolean inGetDefaultEditor = new AtomicBoolean();
		final AtomicBoolean concurrentlyInGetDefaultEditor = new AtomicBoolean();
		final AtomicInteger count = new AtomicInteger();
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				count.incrementAndGet();
				Thread.sleep(500);
				concurrentlyInGetDefaultEditor.set(inGetDefaultEditor.getAndSet(true));
				Thread.sleep(500);
				inGetDefaultEditor.set(false);
				return invocation.callRealMethod();
			}
		}).when(typeConverter).getDefaultEditor(UUID.class);
		beanFactoryTypeConverter.setBeanFactory(beanFactory);
		final TypeDescriptor sourceType = TypeDescriptor.valueOf(UUID.class);
		final TypeDescriptor targetType = TypeDescriptor.valueOf(String.class);
		ExecutorService exec = Executors.newFixedThreadPool(2);
		Runnable test = new Runnable() {
			@Override
			public void run() {
				beanFactoryTypeConverter.canConvert(sourceType, targetType);
				beanFactoryTypeConverter.convertValue(UUID.randomUUID(), sourceType, targetType);
			}
		};
		exec.execute(test);
		exec.execute(test);
		exec.shutdown();
		assertTrue(exec.awaitTermination(10, TimeUnit.SECONDS));
		assertEquals(4, count.get());
		assertFalse(concurrentlyInGetDefaultEditor.get());
	}

	public static class Foo {

	}

	public static class Bar {

	}

	public static class Service {

		public String handle(Map<String, Map<String, Set<Bar>>> payload) {
			assertThat(payload.get("foo").get("foo").iterator().next(), instanceOf(Bar.class));
			return "bar";
		}

		public String handle(Collection<Bar> payload) {
			assertThat(payload.iterator().next(), instanceOf(Bar.class));
			return "baz";
		}
	}
}
