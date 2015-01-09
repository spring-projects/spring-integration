/*
 * Copyright 2015 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.file.splitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

/**
 * The {@link AbstractMessageSplitter} implementation to split the {@link File}
 * Message payload to lines.
 * <p>
 * With {@code iterator = true} (defaults to {@code true}) this class produces an {@link Iterator}
 * to process file lines on demand from {@link Iterator#next}.
 * Otherwise a {@link List} of all lines is returned to the to further
 * {@link AbstractMessageSplitter#handleRequestMessage} process.
 * <p>
 *  Can accept {@link String} as file path, {@link File}, {@link Reader} or {@link InputStream}
 *  as payload type.
 *  All other types are ignored and returned to the {@link AbstractMessageSplitter} as is.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 4.1.2
 */
public class FileSplitter extends AbstractMessageSplitter {

	private final boolean iterator;

	private Charset charset;

	/**
	 * Construct a splitter where the {@link #splitMessage(Message)} method returns
	 * an iterator and the file is read line-by-line during iteration.
	 */
	public FileSplitter() {
		this(true);
	}

	/**
	 * Construct a splitter where the {@link #splitMessage(Message)} method returns
	 * an iterator, and the file is read line-by-line during iteration, or a list
	 * of lines from the file.
	 * @param iterator true to return an iterator, false to return a list of lines.
	 */
	public FileSplitter(boolean iterator) {
		this.iterator = iterator;
	}

	/**
	 * Set the charset to be used when reading the file, when something other than the default
	 * charset is required.
	 * @param charset the charset.
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	@Override
	protected Object splitMessage(final Message<?> message) {
		Object payload = message.getPayload();

		Reader reader = null;

		if (payload instanceof String) {
			try {
				reader = new FileReader((String) payload);
			}
			catch (FileNotFoundException e) {
				throw new MessageHandlingException(message, "failed to read file [" + payload + "]", e);
			}
		}
		else if (payload instanceof File) {
			try {
				if (this.charset == null) {
					reader = new FileReader((File) payload);
				}
				else {
					reader = new InputStreamReader(new FileInputStream((File) payload), this.charset);
				}
			}
			catch (FileNotFoundException e) {
				throw new MessageHandlingException(message, "failed to read file [" + payload + "]", e);
			}
		}
		else if (payload instanceof InputStream) {
			if (this.charset == null) {
				reader = new InputStreamReader((InputStream) payload);
			}
			else {
				reader = new InputStreamReader((InputStream) payload, this.charset);
			}
		}
		else if (payload instanceof Reader) {
			reader = (Reader) payload;
		}
		else {
			return message;
		}

		final BufferedReader bufferedReader = new BufferedReader(reader);
		Iterator<String> iterator = new Iterator<String>() {

			@Override
			public boolean hasNext() {
				try {
					boolean ready = bufferedReader.ready();
					if (!ready) {
						bufferedReader.close();
					}
					return ready;
				}
				catch (IOException e) {
					try {
						bufferedReader.close();
					}
					catch (IOException e1) {}
					throw new MessageHandlingException(message, "IOException while iterating", e);
				}
			}

			@Override
			public String next() {
				try {
					return bufferedReader.readLine();
				}
				catch (IOException e) {
					try {
						bufferedReader.close();
					}
					catch (IOException e1) {}
					throw new MessageHandlingException(message, "IOException while iterating", e);
				}
			}

		};

		if (this.iterator) {
			return iterator;
		}
		else {
			List<String> lines = new ArrayList<String>();
			while (iterator.hasNext()) {
				lines.add(iterator.next());
			}
			return lines;
		}
	}

}
