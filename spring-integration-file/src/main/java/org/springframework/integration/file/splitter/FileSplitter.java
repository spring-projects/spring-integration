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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * The {@link AbstractMessageSplitter} implementation to split the {@link File}
 * Message payload to lines.
 * <p>
 * With {@code iterator = true} (defaults to {@code false}) this class produces an {@link Iterator}
 * to process file lines on demand from {@link Iterator#next}.
 * Otherwise the {@link List} of all lines is returned to the to further
 * {@link AbstractMessageSplitter#handleRequestMessage} process.
 * <p>
 *  Can accept {@link String} as file path, {@link File}, {@link Reader} or {@link InputStream}
 *  as payload type.
 *  All other types are ignored and returned to the {@link AbstractMessageSplitter) as is.
 *
 * @author Artem Bilan
 * @since 4.1.2
 */
public class FileSplitter extends AbstractMessageSplitter {

	private boolean iterator;

	public void setIterator(boolean iterator) {
		this.iterator = iterator;
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
				throw new MessagingException(message, "failed to read file [" + payload + "]", e);
			}
		}
		if (payload instanceof File) {
			try {
				reader = new FileReader((File) payload);
			}
			catch (FileNotFoundException e) {
				throw new MessagingException(message, "failed to read file [" + payload + "]", e);
			}
		}
		else if (payload instanceof InputStream) {
			reader = new InputStreamReader((InputStream) payload);
		}
		else if (payload instanceof Reader) {
			reader = (Reader) payload;
		}
		else {
			return message;
		}

		Iterator<String> iterator = new LineIterator(new BufferedReader(reader));

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

	private class LineIterator implements Iterator<String> {

		private final BufferedReader reader;

		public LineIterator(BufferedReader reader) {
			this.reader = reader;
		}

		@Override
		public boolean hasNext() {
			try {
				boolean ready = reader.ready();
				if (!ready) {
					reader.close();
				}
				return ready;
			}
			catch (IOException e) {
				try {
					reader.close();
				}
				catch (IOException e1) {}
				throw new RuntimeException(e);
			}
		}

		@Override
		public String next() {
			try {
				return reader.readLine();
			}
			catch (IOException e) {
				try {
					reader.close();
				}
				catch (IOException e1) {}
				throw new RuntimeException(e);
			}
		}

	}

}
