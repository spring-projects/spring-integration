/*
 * Copyright 2002-present the original author or authors.
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
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class ConsoleInboundChannelAdapterParserTests {

	private static final ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes());

	@Autowired
	ApplicationContext context;

	@BeforeAll
	public static void setTestInputStream() {
		System.setIn(stream);
	}

	@BeforeEach
	void resetStream() {
		stream.reset();
	}

	@Test
	public void adapterWithDefaultCharset() {
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
		assertThat(TestUtils.<Boolean>getPropertyValue(source, "blockToDetectEOF")).isTrue();
	}

	@Test
	public void adapterWithProvidedCharset() {
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
		assertThat(readerCharset).isEqualTo(StandardCharsets.UTF_8);
		Message<?> message = source.receive();
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
		adapter = context.getBean("pipedAdapterWithCharset.adapter", SourcePollingChannelAdapter.class);
		source = adapter.getMessageSource();
		assertThat(TestUtils.<Boolean>getPropertyValue(source, "blockToDetectEOF")).isTrue();
		bufferedReader = (Reader) sourceAccessor.getPropertyValue("reader");
		assertThat(bufferedReader.getClass()).isEqualTo(BufferedReader.class);
		bufferedReaderAccessor = new DirectFieldAccessor(bufferedReader);
		reader = (Reader) bufferedReaderAccessor.getPropertyValue("in");
		assertThat(reader.getClass()).isEqualTo(InputStreamReader.class);
		readerCharset = Charset.forName(((InputStreamReader) reader).getEncoding());
		assertThat(readerCharset).isEqualTo(StandardCharsets.UTF_8);
	}

	@Test
	public void testConsoleSourceWithInvalidCharset() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("invalidConsoleInboundChannelAdapterParserTests.xml",
								ConsoleInboundChannelAdapterParserTests.class))
				.withRootCauseInstanceOf(UnsupportedEncodingException.class);
	}

}
