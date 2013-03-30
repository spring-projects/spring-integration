/*
 * Copyright 2002-2013 the original author or authors.
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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
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
			public String getComponentName() {
				return "bar";
			}

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

	@Test
	public void testVoidArg() {
		@SuppressWarnings("deprecation")
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		Object foo = typeConverter.convertValue(null, null, TypeDescriptor.valueOf(Void.class));
		assertNull(foo);
		foo = typeConverter.convertValue(null, null, TypeDescriptor.valueOf(Void.TYPE));
		assertNull(foo);
	}

	@Test
	public void testEditorWithTargetString() {
		@SuppressWarnings("deprecation")
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter(conversionService);
		UUID uuid = UUID.randomUUID();
		Object foo = typeConverter.convertValue(uuid, TypeDescriptor.valueOf(UUID.class),
				TypeDescriptor.valueOf(String.class));
		assertEquals(uuid.toString(), foo);
	}

	@Test
	public void testEditorWithTargetFoo() {
		@SuppressWarnings("deprecation")
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		final Foo foo = new Foo();
		conversionService.addConverter(new Converter<String, Foo>() {
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
		@SuppressWarnings("deprecation")
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
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
			assertTrue(payload.get("foo").get("foo").iterator().next() instanceof Bar);
			return "bar";
		}

		public String handle(Collection<Bar> payload) {
			assertTrue(payload.iterator().next() instanceof Bar);
			return "baz";
		}
	}
}
