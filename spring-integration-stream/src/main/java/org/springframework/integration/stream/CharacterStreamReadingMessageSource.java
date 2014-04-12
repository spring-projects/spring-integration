/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * A pollable source for {@link Reader Readers}.
 *
 * @author Mark Fisher
 */
public class CharacterStreamReadingMessageSource extends IntegrationObjectSupport implements MessageSource<String> {

	private final BufferedReader reader;

	private final Object monitor;


	public CharacterStreamReadingMessageSource(Reader reader) {
		this(reader, -1);
	}

	public CharacterStreamReadingMessageSource(Reader reader, int bufferSize) {
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
	}


	public String getComponentType() {
		return "stream:stdin-channel-adapter(character)";
	}

	public Message<String> receive() {
		try {
			synchronized (this.monitor) {
				if (!this.reader.ready()) {
					return null;
				}
				String line = this.reader.readLine();
				return (line != null) ? new GenericMessage<String>(line) : null;
			}
		}
		catch (IOException e) {
			throw new MessagingException("IO failure occurred in adapter", e);
		}
	}


	public static final CharacterStreamReadingMessageSource stdin() {
		return new CharacterStreamReadingMessageSource(new InputStreamReader(System.in));
	}

	public static final CharacterStreamReadingMessageSource stdin(String charsetName) {
		try {
			return new CharacterStreamReadingMessageSource(new InputStreamReader(System.in, charsetName));
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("unsupported encoding: " + charsetName, e);
		}
	}

}
