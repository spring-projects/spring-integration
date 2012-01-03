/*
 * Copyright 2002-2011 the original author or authors.
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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.stream.CharacterStreamWritingMessageHandler;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ConsoleOutboundChannelAdapterParserTests {

	private final ByteArrayOutputStream err = new ByteArrayOutputStream();

	private final ByteArrayOutputStream out = new ByteArrayOutputStream();


	@Before
	public void setupStreams() {
		System.setErr(new PrintStream(this.err));
		System.setOut(new PrintStream(this.out));
	}

	private void resetStreams() {
		this.err.reset();
		this.out.reset();
	}

	@Test
	public void stdoutAdapterWithDefaultCharset() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"consoleOutboundChannelAdapterParserTests.xml", ConsoleOutboundChannelAdapterParserTests.class);
		Object adapter = context.getBean("stdoutAdapterWithDefaultCharset");
		CharacterStreamWritingMessageHandler handler = (CharacterStreamWritingMessageHandler)
				new DirectFieldAccessor(adapter).getPropertyValue("handler");
		DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
		Writer bufferedWriter = (Writer) accessor.getPropertyValue("writer");
		assertEquals(BufferedWriter.class, bufferedWriter.getClass());
		DirectFieldAccessor bufferedWriterAccessor = new DirectFieldAccessor(bufferedWriter);
		Writer writer = (Writer) bufferedWriterAccessor.getPropertyValue("out");
		assertEquals(OutputStreamWriter.class, writer.getClass());
		Charset writerCharset = Charset.forName(((OutputStreamWriter) writer).getEncoding());
		assertEquals(Charset.defaultCharset(), writerCharset);
		this.resetStreams();
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals("foo", out.toString());
		assertEquals("", err.toString());
		assertEquals(23, accessor.getPropertyValue("order"));
	}

	@Test
	public void stdoutAdapterWithProvidedCharset() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"consoleOutboundChannelAdapterParserTests.xml", ConsoleOutboundChannelAdapterParserTests.class);
		Object adapter = context.getBean("stdoutAdapterWithProvidedCharset");
		CharacterStreamWritingMessageHandler handler = (CharacterStreamWritingMessageHandler)
				new DirectFieldAccessor(adapter).getPropertyValue("handler");
		DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
		Writer bufferedWriter = (Writer) accessor.getPropertyValue("writer");
		assertEquals(BufferedWriter.class, bufferedWriter.getClass());
		DirectFieldAccessor bufferedWriterAccessor = new DirectFieldAccessor(bufferedWriter);
		Writer writer = (Writer) bufferedWriterAccessor.getPropertyValue("out");
		assertEquals(OutputStreamWriter.class, writer.getClass());
		Charset writerCharset = Charset.forName(((OutputStreamWriter) writer).getEncoding());
		assertEquals(Charset.forName("UTF-8"), writerCharset);
		this.resetStreams();
		handler.handleMessage(new GenericMessage<String>("bar"));
		assertEquals("bar", out.toString());
		assertEquals("", err.toString());
	}

	@Test
	public void stdoutAdapterWithInvalidCharset() {
		BeanCreationException beanCreationException = null;
		try {
			new ClassPathXmlApplicationContext(
					"invalidConsoleOutboundChannelAdapterParserTests.xml", ConsoleOutboundChannelAdapterParserTests.class);
		}
		catch (BeanCreationException e) {
			beanCreationException = e;
		}
		Throwable rootCause = beanCreationException.getRootCause();
		assertEquals(UnsupportedEncodingException.class, rootCause.getClass());
	}

	@Test
	public void stderrAdapter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"consoleOutboundChannelAdapterParserTests.xml", ConsoleOutboundChannelAdapterParserTests.class);
		Object adapter = context.getBean("stderrAdapter");
		CharacterStreamWritingMessageHandler handler = (CharacterStreamWritingMessageHandler)
				new DirectFieldAccessor(adapter).getPropertyValue("handler");
		DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
		Writer bufferedWriter = (Writer) accessor.getPropertyValue("writer");
		assertEquals(BufferedWriter.class, bufferedWriter.getClass());
		DirectFieldAccessor bufferedWriterAccessor = new DirectFieldAccessor(bufferedWriter);
		Writer writer = (Writer) bufferedWriterAccessor.getPropertyValue("out");
		assertEquals(OutputStreamWriter.class, writer.getClass());
		Charset writerCharset = Charset.forName(((OutputStreamWriter) writer).getEncoding());
		assertEquals(Charset.defaultCharset(), writerCharset);
		this.resetStreams();
		handler.handleMessage(new GenericMessage<String>("bad"));
		assertEquals("", out.toString());
		assertEquals("bad", err.toString());
		assertEquals(34, accessor.getPropertyValue("order"));		
	}

    @Test
	public void stdoutAdatperWithAppendNewLine() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"consoleOutboundChannelAdapterParserTests.xml", ConsoleOutboundChannelAdapterParserTests.class);
		Object adapter = context.getBean("newlineAdapter");
		CharacterStreamWritingMessageHandler handler = (CharacterStreamWritingMessageHandler)
				new DirectFieldAccessor(adapter).getPropertyValue("handler");
		DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
		Writer bufferedWriter = (Writer) accessor.getPropertyValue("writer");
		assertEquals(BufferedWriter.class, bufferedWriter.getClass());
		DirectFieldAccessor bufferedWriterAccessor = new DirectFieldAccessor(bufferedWriter);
		Writer writer = (Writer) bufferedWriterAccessor.getPropertyValue("out");
		assertEquals(OutputStreamWriter.class, writer.getClass());
		Charset writerCharset = Charset.forName(((OutputStreamWriter) writer).getEncoding());
		assertEquals(Charset.defaultCharset(), writerCharset);
		this.resetStreams();
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals("foo" + System.getProperty("line.separator"), out.toString());
	}

	@Test //INT-2275
	public void stdoutInsideNestedChain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"consoleOutboundChannelAdapterParserTests.xml", ConsoleOutboundChannelAdapterParserTests.class);
		DirectChannel channel = context.getBean("stdoutInsideNestedChain", DirectChannel.class);
		this.resetStreams();
		channel.send(new GenericMessage<String>("foo"));
		assertEquals("foobar", out.toString());
	}
}
