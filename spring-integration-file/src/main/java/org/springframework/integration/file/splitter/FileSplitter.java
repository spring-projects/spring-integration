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
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.integration.file.splitter.FileSplitter.FileMarker.Mark;
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

	private final boolean markers;

	private Charset charset;

	/**
	 * Construct a splitter where the {@link #splitMessage(Message)} method returns
	 * an iterator and the file is read line-by-line during iteration.
	 */
	public FileSplitter() {
		this(true, false);
	}

	/**
	 * Construct a splitter where the {@link #splitMessage(Message)} method returns
	 * an iterator, and the file is read line-by-line during iteration, or a list
	 * of lines from the file.
	 * @param iterator true to return an iterator, false to return a list of lines.
	 */
	public FileSplitter(boolean iterator) {
		this(iterator, false);
	}

	/**
	 * Construct a splitter where the {@link #splitMessage(Message)} method returns
	 * an iterator, and the file is read line-by-line during iteration, or a list
	 * of lines from the file. When file markers are enabled (START/END)
	 * {@link #setApplySequence(boolean) applySequence} is false by default. If enabled,
	 * the markers are included in the sequence size.
	 * @param iterator true to return an iterator, false to return a list of lines.
	 * @param markers true to emit start of file/end of file marker messages before/after the data.
	 * @since 1.4.5
	 */
	public FileSplitter(boolean iterator, boolean markers) {
		this.iterator = iterator;
		this.markers = markers;
		if (markers) {
			setApplySequence(false);
		}
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

		final String filePath;

		if (payload instanceof String) {
			try {
				reader = new FileReader((String) payload);
				filePath = (String) payload;
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
				filePath = ((File) payload).getAbsolutePath();
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
			filePath = ":stream:";
		}
		else if (payload instanceof Reader) {
			reader = (Reader) payload;
			filePath = ":reader:";
		}
		else {
			return message;
		}

		final BufferedReader bufferedReader = new BufferedReader(reader);
		Iterator<Object> iterator = new Iterator<Object>() {

			boolean markers = FileSplitter.this.markers;

			boolean sof = markers;

			boolean eof;

			boolean done;

			@Override
			public boolean hasNext() {
				try {
					boolean ready = !this.done && bufferedReader.ready();
					if (!ready) {
						if (this.markers) {
							this.eof = true;
						}
						bufferedReader.close();
					}
					return this.sof || ready || this.eof;
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
			public Object next() {
				if (this.sof) {
					this.sof = false;
					return new FileMarker(filePath, Mark.START);
				}
				if (this.eof) {
					this.eof = false;
					this.markers = false;
					this.done = true;
					return new FileMarker(filePath, Mark.END);
				}
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
			List<Object> lines = new ArrayList<Object>();
			while (iterator.hasNext()) {
				lines.add(iterator.next());
			}
			return lines;
		}
	}

	public static class FileMarker implements Serializable {

		private static final long serialVersionUID = 8514605438145748406L;

		public enum Mark implements Serializable {
			START,
			END
		}

		private final String filePath;

		private final Mark mark;

		public FileMarker(String filePath, Mark mark) {
			this.filePath = filePath;
			this.mark = mark;
		}

		public String getFilePath() {
			return filePath;
		}

		public Mark getMark() {
			return mark;
		}

		@Override
		public String toString() {
			return "FileMarker [filePath=" + filePath + ", mark=" + mark + "]";
		}

	}

}
