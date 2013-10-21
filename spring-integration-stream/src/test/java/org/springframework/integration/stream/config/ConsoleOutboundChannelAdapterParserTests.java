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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.stream.CharacterStreamWritingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ConsoleOutboundChannelAdapterParserTests {

	@Autowired
	@Qualifier("stdoutAdapterWithDefaultCharset.handler")
	private MessageHandler stdoutAdapterWithDefaultCharsetHandler;

	@Autowired
	@Qualifier("stdoutAdapterWithProvidedCharset.handler")
	private MessageHandler stdoutAdapterWithProvidedCharsetHandler;

	@Autowired
	@Qualifier("stderrAdapter.handler")
	private MessageHandler stderrAdapterHandler;

	@Autowired
	@Qualifier("newlineAdapter.handler")
	private MessageHandler newlineAdapterHandler;

	@Autowired
	@Qualifier("stdoutChain.handler")
	private MessageHandler stdoutChainHandler;

	@Autowired
	private MessageChannel stdoutInsideNestedChain;

	@Test
	public void stdoutAdapterWithDefaultCharset() throws IOException {
		BufferedWriter bufferedWriter = TestUtils.getPropertyValue(this.stdoutAdapterWithDefaultCharsetHandler, "writer", BufferedWriter.class);
		Writer writer = TestUtils.getPropertyValue(bufferedWriter, "out", Writer.class);
		assertEquals(OutputStreamWriter.class, writer.getClass());
		Charset writerCharset = Charset.forName(((OutputStreamWriter) writer).getEncoding());
		assertEquals(Charset.defaultCharset(), writerCharset);

		Object lock = TestUtils.getPropertyValue(writer, "lock");
		assertEquals(System.out, lock);

		bufferedWriter = Mockito.spy(bufferedWriter);

		DirectFieldAccessor dfa = new DirectFieldAccessor(this.stdoutAdapterWithDefaultCharsetHandler);
		dfa.setPropertyValue("writer", bufferedWriter);

		this.stdoutAdapterWithDefaultCharsetHandler.handleMessage(new GenericMessage<String>("foo"));

		verify(bufferedWriter, times(1)).write(eq("foo"));
		assertEquals(23, TestUtils.getPropertyValue(this.stdoutAdapterWithDefaultCharsetHandler, "order"));
	}

	@Test
	public void stdoutAdapterWithProvidedCharset() throws IOException {
		BufferedWriter bufferedWriter = TestUtils.getPropertyValue(this.stdoutAdapterWithProvidedCharsetHandler, "writer", BufferedWriter.class);
		Writer writer = TestUtils.getPropertyValue(bufferedWriter, "out", Writer.class);
		assertEquals(OutputStreamWriter.class, writer.getClass());
		Charset writerCharset = Charset.forName(((OutputStreamWriter) writer).getEncoding());
		assertEquals(Charset.forName("UTF-8"), writerCharset);

		Object lock = TestUtils.getPropertyValue(writer, "lock");
		assertEquals(System.out, lock);

		bufferedWriter = Mockito.spy(bufferedWriter);

		DirectFieldAccessor dfa = new DirectFieldAccessor(this.stdoutAdapterWithProvidedCharsetHandler);
		dfa.setPropertyValue("writer", bufferedWriter);

		this.stdoutAdapterWithProvidedCharsetHandler.handleMessage(new GenericMessage<String>("bar"));

		verify(bufferedWriter, times(1)).write(eq("bar"));
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
	public void stderrAdapter() throws IOException {
		BufferedWriter bufferedWriter = TestUtils.getPropertyValue(this.stderrAdapterHandler, "writer", BufferedWriter.class);
		Writer writer = TestUtils.getPropertyValue(bufferedWriter, "out", Writer.class);
		assertEquals(OutputStreamWriter.class, writer.getClass());
		Charset writerCharset = Charset.forName(((OutputStreamWriter) writer).getEncoding());
		assertEquals(Charset.defaultCharset(), writerCharset);

		Object lock = TestUtils.getPropertyValue(writer, "lock");
		assertEquals(System.err, lock);

		bufferedWriter = Mockito.spy(bufferedWriter);

		DirectFieldAccessor dfa = new DirectFieldAccessor(this.stderrAdapterHandler);
		dfa.setPropertyValue("writer", bufferedWriter);


		this.stderrAdapterHandler.handleMessage(new GenericMessage<String>("bar"));

		verify(bufferedWriter, times(1)).write(eq("bar"));

		assertEquals(34, TestUtils.getPropertyValue(this.stderrAdapterHandler, "order"));
	}

    @Test
	public void stdoutAdatperWithAppendNewLine() throws IOException {
		BufferedWriter bufferedWriter = TestUtils.getPropertyValue(this.newlineAdapterHandler, "writer", BufferedWriter.class);
		Writer writer = TestUtils.getPropertyValue(bufferedWriter, "out", Writer.class);
		assertEquals(OutputStreamWriter.class, writer.getClass());
		Charset writerCharset = Charset.forName(((OutputStreamWriter) writer).getEncoding());
		assertEquals(Charset.defaultCharset(), writerCharset);

		Object lock = TestUtils.getPropertyValue(writer, "lock");
		assertEquals(System.out, lock);

		bufferedWriter = Mockito.spy(bufferedWriter);

		DirectFieldAccessor dfa = new DirectFieldAccessor(this.newlineAdapterHandler);
		dfa.setPropertyValue("writer", bufferedWriter);

		this.newlineAdapterHandler.handleMessage(new GenericMessage<String>("bar"));

		verify(bufferedWriter, times(1)).write(eq("bar"));
		verify(bufferedWriter, times(1)).newLine();

	}

	@Test //INT-2275
	public void stdoutInsideNestedChain() throws IOException {
		List<?> handlers = TestUtils.getPropertyValue(this.stdoutChainHandler, "handlers", List.class);
		assertEquals(2, handlers.size());
		Object chainHandler = handlers.get(1);
		assertTrue(chainHandler instanceof MessageHandlerChain);
		List<?> nestedChainHandlers = TestUtils.getPropertyValue(chainHandler, "handlers", List.class);
		assertEquals(1, nestedChainHandlers.size());
		Object stdoutHandler = nestedChainHandlers.get(0);
		assertTrue(stdoutHandler instanceof CharacterStreamWritingMessageHandler);

		BufferedWriter bufferedWriter = TestUtils.getPropertyValue(stdoutHandler, "writer", BufferedWriter.class);
		Writer writer = TestUtils.getPropertyValue(bufferedWriter, "out", Writer.class);
		assertEquals(OutputStreamWriter.class, writer.getClass());
		Charset writerCharset = Charset.forName(((OutputStreamWriter) writer).getEncoding());
		assertEquals(Charset.defaultCharset(), writerCharset);

		Object lock = TestUtils.getPropertyValue(writer, "lock");
		assertEquals(System.out, lock);

		bufferedWriter = Mockito.spy(bufferedWriter);

		DirectFieldAccessor dfa = new DirectFieldAccessor(stdoutHandler);
		dfa.setPropertyValue("writer", bufferedWriter);

		this.stdoutInsideNestedChain.send(new GenericMessage<String>("foo"));
		verify(bufferedWriter, times(1)).write(eq("foobar"));
	}
}
