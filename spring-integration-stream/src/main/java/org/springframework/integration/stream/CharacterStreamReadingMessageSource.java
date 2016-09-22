/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * A pollable source for {@link Reader Readers}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class CharacterStreamReadingMessageSource extends IntegrationObjectSupport implements MessageSource<String> {

	private final BufferedReader reader;

	private final Object monitor;

	private final boolean blockAndCloseContext;

	/**
	 * Construct an instance with the provider reader.
	 * {@link #receive()} will return {@code null} when the reader is not ready.
	 * @param reader the reader.
	 */
	public CharacterStreamReadingMessageSource(Reader reader) {
		this(reader, -1, false);
	}

	/**
	 * Construct an instance with the provider reader and buffer size.
	 * {@link #receive()} will return {@code null} when the reader is not ready.
	 * @param reader the reader.
	 * @param bufferSize the buffer size.
	 */
	public CharacterStreamReadingMessageSource(Reader reader, int bufferSize) {
		this(reader, bufferSize, false);
	}

	/**
	 * Construct an instance with the provider reader and buffer size.
	 * When {@code blockReceive} is {@code false},
	 * {@link #receive()} will return {@code null} when the reader is not ready.
	 * When it is {@code true}, the thread will block until data is available; when the
	 * underlying stream is closed, the application context (if present) will be closed.
	 * This can be useful, for example, when piping stdin
	 * <pre class="code">
	 *     cat foo.txt | java -jar my.jar
	 * </pre>
	 * or
	 * <pre class="code">
	 *     java -jar my.jar < foo.txt
	 * </pre>
	 * @param reader the reader.
	 * @param bufferSize the buffer size; if negative use the default in
	 * {@link BufferedReader}.
	 * @param blockAndCloseContext true to block the thread until data is available and
	 * close the context at EOF.
	 * @since 5.0
	 */
	public CharacterStreamReadingMessageSource(Reader reader, int bufferSize, boolean blockAndCloseContext) {
		Assert.notNull(reader, "reader must not be null");
		this.monitor = reader;
		if (reader instanceof BufferedReader) {
			this.reader = (BufferedReader) reader;
		}
		else if (bufferSize > 0) {
			this.reader = new BufferedReader(reader, bufferSize);
		}
		else {
			this.reader = new BufferedReader(reader);
		}
		this.blockAndCloseContext = blockAndCloseContext;
	}


	@Override
	public String getComponentType() {
		return "stream:stdin-channel-adapter(character)";
	}

	@Override
	public Message<String> receive() {
		try {
			synchronized (this.monitor) {
				if (!this.blockAndCloseContext && !this.reader.ready()) {
					return null;
				}
				String line = this.reader.readLine();
				if (line == null) {
					ConfigurableApplicationContext context = (ConfigurableApplicationContext) getApplicationContext();
					if (context != null) {
						this.logger.info("EOF on stream; closing context");
						context.close();
					}
				}
				return (line != null) ? new GenericMessage<String>(line) : null;
			}
		}
		catch (IOException e) {
			throw new MessagingException("IO failure occurred in adapter", e);
		}
	}


	/**
	 * Create a source that reads from {@link System#in}. EOF will not be detected.
	 * @return the stream.
	 */
	public static final CharacterStreamReadingMessageSource stdin() {
		return new CharacterStreamReadingMessageSource(new InputStreamReader(System.in));
	}

	/**
	 * Create a source that reads from {@link System#in}. EOF will not be detected.
	 * @param charsetName the charset to use when converting bytes to String.
	 * @return the stream.
	 */
	public static final CharacterStreamReadingMessageSource stdin(String charsetName) {
		try {
			return new CharacterStreamReadingMessageSource(new InputStreamReader(System.in, charsetName));
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("unsupported encoding: " + charsetName, e);
		}
	}

	/**
	 * Create a source that reads from {@link System#in}. EOF will be detected and the application
	 * context closed.
	 * @return the stream.
	 * @see CharacterStreamReadingMessageSource#CharacterStreamReadingMessageSource(Reader, int, boolean)
	 */
	public static final CharacterStreamReadingMessageSource stdinPipe() {
		return new CharacterStreamReadingMessageSource(new InputStreamReader(System.in), -1, true);
	}

	/**
	 * Create a source that reads from {@link System#in}. EOF will be detected and the application
	 * context closed.
	 * @param charsetName the charset to use when converting bytes to String.
	 * @return the stream.
	 * @see CharacterStreamReadingMessageSource#CharacterStreamReadingMessageSource(Reader, int, boolean)
	 */
	public static final CharacterStreamReadingMessageSource stdinPipe(String charsetName) {
		try {
			return new CharacterStreamReadingMessageSource(new InputStreamReader(System.in, charsetName), -1, true);
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("unsupported encoding: " + charsetName, e);
		}
	}

}
