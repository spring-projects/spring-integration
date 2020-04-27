/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.file.splitter;

import java.io.BufferedReader;
import java.io.Closeable;
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
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.splitter.FileSplitter.FileMarker.Mark;
import org.springframework.integration.json.SimpleJsonSerializer;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.util.CloseableIterator;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The {@link AbstractMessageSplitter} implementation to split the {@link File} Message
 * payload to lines.
 * <p>
 * With {@code iterator = true} (defaults to {@code true}) this class produces an
 * {@link Iterator} to process file lines on demand from {@link Iterator#next}. Otherwise
 * a {@link List} of all lines is returned to the to further
 * {@link AbstractMessageSplitter#handleRequestMessage} process.
 * <p>
 * Can accept {@link String} as file path, {@link File}, {@link Reader} or
 * {@link InputStream} as payload type. All other types are ignored and returned to the
 * {@link AbstractMessageSplitter} as is.
 * <p>
 * If {@link #setFirstLineAsHeader(String)} is specified, the first line of the content is
 * treated as a header and carried as a header with the provided name in the messages
 * emitted for the remaining lines. In this case, if markers are enabled, the line count
 * in the END marker does not include the header line and, if
 * {@link #setApplySequence(boolean) applySequence} is true, the header is not included in
 * the sequence.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Ruslan Stelmachenko
 *
 * @since 4.1.2
 */
public class FileSplitter extends AbstractMessageSplitter {

	private final boolean returnIterator;

	private final boolean markers;

	private final boolean markersJson;

	@Nullable
	private Charset charset;

	private String firstLineHeaderName;

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
	 * @since 4.1.5
	 */
	public FileSplitter(boolean iterator, boolean markers) {
		this(iterator, markers, false);
	}

	/**
	 * Construct a splitter where the {@link #splitMessage(Message)} method returns an
	 * iterator, and the file is read line-by-line during iteration, or a list of lines
	 * from the file. When file markers are enabled (START/END)
	 * {@link #setApplySequence(boolean) applySequence} is false by default. If enabled,
	 * the markers are included in the sequence size.
	 * @param iterator true to return an iterator, false to return a list of lines.
	 * @param markers true to emit start of file/end of file marker messages before/after
	 * the data.
	 * @param markersJson when true, markers are represented as JSON.
	 * @since 4.2.7
	 */
	public FileSplitter(boolean iterator, boolean markers, boolean markersJson) {
		this.returnIterator = iterator;
		this.markers = markers;
		if (markers) {
			setApplySequence(false);
		}
		this.markersJson = markersJson;
	}

	/**
	 * Set the charset to be used when reading the file, when something other than the default
	 * charset is required.
	 * @param charset the charset.
	 */
	public void setCharset(@Nullable Charset charset) {
		this.charset = charset;
	}

	/**
	 * Specify the header name for the first line to be carried as a header in the
	 * messages emitted for the remaining lines.
	 * @param firstLineHeaderName the header name to carry first line.
	 * @since 5.0
	 */
	public void setFirstLineAsHeader(String firstLineHeaderName) {
		Assert.hasText(firstLineHeaderName, "'firstLineHeaderName' must not be empty");
		this.firstLineHeaderName = firstLineHeaderName;
	}

	@Override// NOSONAR complexity
	protected Object splitMessage(final Message<?> message) {
		Object payload = message.getPayload();

		Reader reader;

		String filePath;

		if (payload instanceof String) {
			try {
				reader = new FileReader((String) payload);
				filePath = (String) payload;
			}
			catch (FileNotFoundException e) {
				throw new MessageHandlingException(message,
						"Error handing message in the [" + this + "]. Failed to read file [" + payload + "]", e);
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
			filePath = buildPathFromMessage(message, ":stream:");
		}
		else if (payload instanceof Reader) {
			reader = (Reader) payload;
			filePath = buildPathFromMessage(message, ":reader:");
		}
		else {
			return message;
		}

		Iterator<Object> iterator = messageToFileIterator(message, reader, filePath);

		if (this.returnIterator) {
			return iterator;
		}
		else {
			List<Object> lines = new ArrayList<>();
			while (iterator.hasNext()) {
				lines.add(iterator.next());
			}
			return lines;
		}
	}

	private Iterator<Object> messageToFileIterator(Message<?> message, Reader reader, String filePath) {
		BufferedReader bufferedReader = wrapToBufferedReader(message, reader);

		String firstLineAsHeader = null;

		if (this.firstLineHeaderName != null) {
			try {
				firstLineAsHeader = bufferedReader.readLine();
			}
			catch (IOException e) {
				throw new MessageHandlingException(message, "IOException while reading first line", e);
			}
		}

		return new FileIterator(message, bufferedReader, firstLineAsHeader, filePath);
	}

	private BufferedReader wrapToBufferedReader(Message<?> message, Reader reader) {
		return new BufferedReader(reader) {

			@Override
			public void close() throws IOException {
				try {
					super.close();
				}
				finally {
					Closeable closeableResource = StaticMessageHeaderAccessor.getCloseableResource(message);
					if (closeableResource != null) {
						closeableResource.close();
					}
				}
			}

		};
	}

	@Override
	protected boolean willAddHeaders(Message<?> message) {
		Object payload = message.getPayload();
		return payload instanceof File || payload instanceof String;
	}

	@Override
	protected void addHeaders(Message<?> message, Map<String, Object> headers) {
		File file = null;
		if (message.getPayload() instanceof File) {
			file = (File) message.getPayload();
		}
		else if (message.getPayload() instanceof String) {
			file = new File((String) message.getPayload());
		}
		if (file != null) {
			if (!headers.containsKey(FileHeaders.ORIGINAL_FILE)) {
				headers.put(FileHeaders.ORIGINAL_FILE, file);
			}
			if (!headers.containsKey(FileHeaders.FILENAME)) {
				headers.put(FileHeaders.FILENAME, file.getName());
			}
		}
	}

	private String buildPathFromMessage(Message<?> message, String defaultPath) {
		String remoteDir = (String) message.getHeaders().get(FileHeaders.REMOTE_DIRECTORY);
		String remoteFile = (String) message.getHeaders().get(FileHeaders.REMOTE_FILE);
		if (StringUtils.hasText(remoteDir) && StringUtils.hasText(remoteFile)) {
			return remoteDir + remoteFile;
		}
		else {
			return defaultPath;
		}
	}

	private class FileIterator implements CloseableIterator<Object> {

		private final Message<?> message;

		private final BufferedReader bufferedReader;

		private final String firstLineAsHeader;

		private final String filePath;

		private boolean markers = FileSplitter.this.markers;

		private boolean sof = this.markers;

		private boolean eof;

		private boolean done;

		private String line;

		private long lineCount;

		private boolean hasNextCalled;

		FileIterator(Message<?> message, BufferedReader bufferedReader, String firstLineAsHeader,
				String filePath) {

			this.message = message;
			this.bufferedReader = bufferedReader;
			this.firstLineAsHeader = firstLineAsHeader;
			this.filePath = filePath;
		}

		@Override
		public boolean hasNext() {
			this.hasNextCalled = true;
			try {
				return hasNextLine();
			}
			catch (IOException e) {
				try {
					this.done = true;
					this.bufferedReader.close();
				}
				catch (IOException e1) {
					// ignored
				}
				throw new MessageHandlingException(this.message, "IOException while iterating", e);
			}
		}

		private boolean hasNextLine() throws IOException {
			if (!this.done && this.line == null) {
				this.line = this.bufferedReader.readLine();
			}
			boolean ready = !this.done && this.line != null;
			if (!ready) {
				if (this.markers) {
					this.eof = true;
					if (this.sof) {
						this.done = true;
					}
				}
				this.bufferedReader.close();
			}
			return this.sof || ready || this.eof;
		}

		@Override
		public Object next() {
			if (!this.hasNextCalled) {
				hasNext();
			}
			this.hasNextCalled = false;
			if (this.sof) {
				this.sof = false;
				return markerToReturn(new FileMarker(this.filePath, Mark.START, 0));
			}
			if (this.eof) {
				this.eof = false;
				this.markers = false;
				this.done = true;
				return markerToReturn(new FileMarker(this.filePath, Mark.END, this.lineCount));
			}
			if (this.line != null) {
				String payload = this.line;
				this.line = null;
				this.lineCount++;

				AbstractIntegrationMessageBuilder<String> messageBuilder =
						getMessageBuilderFactory()
								.withPayload(payload);

				if (this.firstLineAsHeader != null) {
					messageBuilder.setHeader(FileSplitter.this.firstLineHeaderName, this.firstLineAsHeader);
				}

				return messageBuilder;
			}
			else {
				this.done = true;
				throw new NoSuchElementException(this.filePath + " has been consumed");
			}
		}

		private AbstractIntegrationMessageBuilder<Object> markerToReturn(FileMarker fileMarker) {
			Object payload;
			if (FileSplitter.this.markersJson) {
				try {
					payload = SimpleJsonSerializer.toJson(fileMarker);
				}
				catch (Exception e) {
					throw new MessageHandlingException(this.message, "Failed to convert marker to JSON", e);
				}
			}
			else {
				payload = fileMarker;
			}
			return getMessageBuilderFactory().withPayload(payload)
					.setHeader(FileHeaders.MARKER, fileMarker.mark.name());
		}

		@Override
		public void close() {
			try {
				this.done = true;
				this.bufferedReader.close();
			}
			catch (IOException e) {
				// ignored
			}
		}

	}

	public static class FileMarker implements Serializable {

		private static final long serialVersionUID = 8514605438145748406L;

		public enum Mark {
			START,
			END
		}

		private final String filePath;

		private final Mark mark;

		private final long lineCount;

		/*
		 * Provided solely to allow deserialization from JSON
		 */
		public FileMarker() {
			this.filePath = null;
			this.mark = null;
			this.lineCount = 0;
		}

		public FileMarker(String filePath, Mark mark, long lineCount) {
			this.filePath = filePath;
			this.mark = mark;
			this.lineCount = lineCount;
		}

		public String getFilePath() {
			return this.filePath;
		}

		public Mark getMark() {
			return this.mark;
		}

		public long getLineCount() {
			return this.lineCount;
		}

		@Override
		public String toString() {
			if (Mark.START.equals(this.mark)) {
				return "FileMarker [filePath=" + this.filePath + ", mark=" + this.mark + "]";
			}
			else {
				return "FileMarker [filePath=" + this.filePath + ", mark=" + this.mark +
						", lineCount=" + this.lineCount + "]";
			}
		}

	}

}
