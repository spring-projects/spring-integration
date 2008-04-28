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

package org.springframework.integration.adapter.stream.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.stream.CharacterStreamSource;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class ConsoleSourceParserTests {

	@Before
	public void writeTestInput() {
		ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes());
		System.setIn(stream);
	}

	@Test
	public void testConsoleSourceWithDefaultCharset() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"consoleSourceParserTests.xml", ConsoleSourceParserTests.class);
		CharacterStreamSource source =
				(CharacterStreamSource) context.getBean("sourceWithDefaultCharset");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(source);
		Reader bufferedReader = (Reader) sourceAccessor.getPropertyValue("reader");
		assertEquals(BufferedReader.class, bufferedReader.getClass());
		DirectFieldAccessor bufferedReaderAccessor = new DirectFieldAccessor(bufferedReader);
		Reader reader = (Reader) bufferedReaderAccessor.getPropertyValue("in");
		assertEquals(InputStreamReader.class, reader.getClass());
		Charset readerCharset = Charset.forName(((InputStreamReader) reader).getEncoding());
		assertEquals(Charset.defaultCharset(), readerCharset);
		Message<?> message = source.receive();
		assertNotNull(message);
		assertEquals("foo", message.getPayload());
	}

	@Test
	public void testConsoleSourceWithProvidedCharset() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"consoleSourceParserTests.xml", ConsoleSourceParserTests.class);
		CharacterStreamSource source =
				(CharacterStreamSource) context.getBean("sourceWithProvidedCharset");
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
					"invalidConsoleSourceParserTests.xml", ConsoleSourceParserTests.class);
		}
		catch (BeanCreationException e) {
			beanCreationException = e;
		}
		Throwable parentCause = beanCreationException.getCause().getCause();
		assertEquals(ConfigurationException.class, parentCause.getClass());
		Throwable configurationExceptionCause = ((ConfigurationException) parentCause).getCause();
		assertEquals(UnsupportedEncodingException.class, configurationExceptionCause.getClass());
	}

}
