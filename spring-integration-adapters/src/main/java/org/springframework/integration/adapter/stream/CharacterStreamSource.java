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

package org.springframework.integration.adapter.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.springframework.integration.adapter.PollableSource;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * A pollable source for {@link Reader Readers}.
 * 
 * @author Mark Fisher
 */
public class CharacterStreamSource implements PollableSource<String> {

	private final BufferedReader reader;

	private final Object monitor;


	public CharacterStreamSource(Reader reader) {
		this(reader, -1);
	}

	public CharacterStreamSource(Reader reader, int bufferSize) {
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


	public String poll() {
		try {
			synchronized (this.monitor) {
				if (!this.reader.ready()) {
					return null;
				}
				return this.reader.readLine();
			}
		}
		catch (IOException e) {
			throw new MessagingException("IO failure occurred in adapter", e);
		}
	}

}
