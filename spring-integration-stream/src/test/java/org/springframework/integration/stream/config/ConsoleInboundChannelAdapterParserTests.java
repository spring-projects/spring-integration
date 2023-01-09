/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.stream.config;

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
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
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
		SourcePollingChannelAdapter adapter = context.getBean("adapterWithDefaultCharset.adapter",
				SourcePollingChannelAdapter.class);
		MessageSource<?> source = (MessageSource<?>) new DirectFieldAccessor(adapter).getPropertyValue("source");
		assertThat(source instanceof NamedComponent).isTrue();
		assertThat(adapter.getComponentName()).isEqualTo("adapterWithDefaultCharset.adapter");
		assertThat(adapter.getComponentType()).isEqualTo("stream:stdin-channel-adapter(character)");
		assertThat(((NamedComponent) source).getComponentType()).isEqualTo("stream:stdin-channel-adapter(character)");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(source);
		Reader bufferedReader = (Reader) sourceAccessor.getPropertyValue("reader");
		assertThat(bufferedReader.getClass()).isEqualTo(BufferedReader.class);
		DirectFieldAccessor bufferedReaderAccessor = new DirectFieldAccessor(bufferedReader);
		Reader reader = (Reader) bufferedReaderAccessor.getPropertyValue("in");
		assertThat(reader.getClass()).isEqualTo(InputStreamReader.class);
		Charset readerCharset = Charset.forName(((InputStreamReader) reader).getEncoding());
		assertThat(readerCharset).isEqualTo(Charset.defaultCharset());
		Message<?> message = source.receive();
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
		adapter = context.getBean("pipedAdapterNoCharset.adapter", SourcePollingChannelAdapter.class);
		source = adapter.getMessageSource();
		assertThat(TestUtils.getPropertyValue(source, "blockToDetectEOF", Boolean.class)).isTrue();
		context.close();
	}

	@Test
	public void adapterWithProvidedCharset() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"consoleInboundChannelAdapterParserTests.xml", ConsoleInboundChannelAdapterParserTests.class);
		SourcePollingChannelAdapter adapter = context.getBean("adapterWithProvidedCharset.adapter",
				SourcePollingChannelAdapter.class);
		MessageSource<?> source = adapter.getMessageSource();
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(source);
		Reader bufferedReader = (Reader) sourceAccessor.getPropertyValue("reader");
		assertThat(bufferedReader.getClass()).isEqualTo(BufferedReader.class);
		assertThat(sourceAccessor.getPropertyValue("blockToDetectEOF")).isEqualTo(false);
		DirectFieldAccessor bufferedReaderAccessor = new DirectFieldAccessor(bufferedReader);
		Reader reader = (Reader) bufferedReaderAccessor.getPropertyValue("in");
		assertThat(reader.getClass()).isEqualTo(InputStreamReader.class);
		Charset readerCharset = Charset.forName(((InputStreamReader) reader).getEncoding());
		assertThat(readerCharset).isEqualTo(Charset.forName("UTF-8"));
		Message<?> message = source.receive();
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
		adapter = context.getBean("pipedAdapterWithCharset.adapter", SourcePollingChannelAdapter.class);
		source = adapter.getMessageSource();
		assertThat(TestUtils.getPropertyValue(source, "blockToDetectEOF", Boolean.class)).isTrue();
		bufferedReader = (Reader) sourceAccessor.getPropertyValue("reader");
		assertThat(bufferedReader.getClass()).isEqualTo(BufferedReader.class);
		bufferedReaderAccessor = new DirectFieldAccessor(bufferedReader);
		reader = (Reader) bufferedReaderAccessor.getPropertyValue("in");
		assertThat(reader.getClass()).isEqualTo(InputStreamReader.class);
		readerCharset = Charset.forName(((InputStreamReader) reader).getEncoding());
		assertThat(readerCharset).isEqualTo(Charset.forName("UTF-8"));
		context.close();
	}

	@Test
	public void testConsoleSourceWithInvalidCharset() {
		BeanCreationException beanCreationException = null;
		try {
			new ClassPathXmlApplicationContext("invalidConsoleInboundChannelAdapterParserTests.xml",
					ConsoleInboundChannelAdapterParserTests.class).close();
		}
		catch (BeanCreationException e) {
			beanCreationException = e;
		}
		Throwable rootCause = beanCreationException.getRootCause();
		assertThat(rootCause.getClass()).isEqualTo(UnsupportedEncodingException.class);
	}

}
