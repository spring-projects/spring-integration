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

package org.springframework.integration.stream.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
public class ConsoleInboundChannelAdapterParserTests {

	@Before
	public void writeTestInput() {
		ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes());
		System.setIn(stream);
	}

	@Test
	public void adapterWithDefaultCharset() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"consoleInboundChannelAdapterParserTests.xml", ConsoleInboundChannelAdapterParserTests.class);
		SourcePollingChannelAdapter adapter =
				(SourcePollingChannelAdapter) context.getBean("adapterWithDefaultCharset.adapter");
		MessageSource<?> source = (MessageSource<?>) new DirectFieldAccessor(adapter).getPropertyValue("source");
		assertTrue(source instanceof NamedComponent);
		assertEquals("adapterWithDefaultCharset.adapter", adapter.getComponentName());
		assertEquals("stream:stdin-channel-adapter(character)", adapter.getComponentType());
		assertEquals("stream:stdin-channel-adapter(character)", ((NamedComponent)source).getComponentType());
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(source);
		Reader bufferedReader = (Reader) sourceAccessor.getPropertyValue("reader");
		assertEquals(BufferedReader.class, bufferedReader.getClass());
		DirectFieldAccessor bufferedReaderAccessor = new DirectFieldAccessor(bufferedReader);
		Reader reader = (Reader) bufferedReaderAccessor.getPropertyValue("in");
		assertEquals(InputStreamReader.class, reader.getClass());
		Charset readerCharset = Charset.forName(((InputStreamReader) reader).getEncoding());
		assertEquals(Charset.defaultCharset(), readerCharset);
		Message<?> message = source.receive();
		System.out.println(message);
		assertNotNull(message);
		assertEquals("foo", message.getPayload());
	}

	@Test
	public void adapterWithProvidedCharset() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"consoleInboundChannelAdapterParserTests.xml", ConsoleInboundChannelAdapterParserTests.class);
		SourcePollingChannelAdapter adapter =
				(SourcePollingChannelAdapter) context.getBean("adapterWithProvidedCharset.adapter");
		MessageSource<?> source = (MessageSource<?>) new DirectFieldAccessor(adapter).getPropertyValue("source");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(source);
		Reader bufferedReader = (Reader) sourceAccessor.getPropertyValue("reader");
		assertEquals(BufferedReader.class, bufferedReader.getClass());
		DirectFieldAccessor bufferedReaderAccessor = new DirectFieldAccessor(bufferedReader);
		Reader reader = (Reader) bufferedReaderAccessor.getPropertyValue("in");
		assertEquals(InputStreamReader.class, reader.getClass());
		Charset readerCharset = Charset.forName(((InputStreamReader) reader).getEncoding());
		assertEquals(Charset.forName("UTF-8"), readerCharset);
		Message<?> message = source.receive();
		assertNotNull(message);
		assertEquals("foo", message.getPayload());
	}

	@Test
	public void testConsoleSourceWithInvalidCharset() {
		BeanCreationException beanCreationException = null;
		try {
			new ClassPathXmlApplicationContext(
					"invalidConsoleInboundChannelAdapterParserTests.xml", ConsoleInboundChannelAdapterParserTests.class);
		}
		catch (BeanCreationException e) {
			beanCreationException = e;
		}
		Throwable rootCause = beanCreationException.getRootCause();
		assertEquals(UnsupportedEncodingException.class, rootCause.getClass());
	}

}
