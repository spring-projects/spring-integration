/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.file.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * An {@link AbstractRemoteFileStreamingMessageSource} that emits individual
 * lines.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public abstract class AbstractLineByLineRemoteFileMessageSource<F> extends AbstractRemoteFileStreamingMessageSource<F> {

	private volatile byte[] lineDelimiter = new byte[] { '\n' };

	private volatile boolean includeDelimiters = true;

	private volatile Session<F> session;

	private volatile InputStream inputStream;

	private AbstractFileInfo<F> currentFile;

	protected AbstractLineByLineRemoteFileMessageSource(RemoteFileTemplate<F> template,
			Comparator<AbstractFileInfo<F>> comparator) {
		super(template, comparator);
	}

	/**
	 * When present, a line delimiter causes the message source to emit individual
	 * lines instead of the whole file; maximum of two bytes. Default '\n'.
	 * @param lineDelimiter the line delimiter.
	 */
	public void setLineDelimiter(byte[] lineDelimiter) {
		Assert.isTrue(lineDelimiter != null ? lineDelimiter.length < 3 : true,
				"'lineDelimiter' can be 2 bytes maximum");
		if (lineDelimiter != null) {
			this.lineDelimiter = lineDelimiter.length > 0 ? lineDelimiter : null;
		}
	}

	/**
	 * Set to true to include line delimiters in lines; false otherwise; default true.
	 * @param includeDelimiters true to include.
	 */
	public void setIncludeDelimiters(boolean includeDelimiters) {
		this.includeDelimiters = includeDelimiters;
	}

	@Override
	protected synchronized Object doReceive() {
		try {
			byte[] nextLine;
			if (this.session != null) {
				nextLine = nextLine();
				while (nextLine == null) {
					nextLine = firstLineFromNextFile();
				}
			}
			else {
				nextLine = firstLineFromNextFile();
			}
			if (nextLine != null) {
				AbstractIntegrationMessageBuilder<byte[]> builder = getMessageBuilderFactory().withPayload(nextLine);
				addHeaders(this.currentFile, builder);
				return builder.build();
			}
			else {
				return null;
			}
		}
		catch (IOException e) {
			if (this.session != null) {
				releaseSession();
			}
			throw new MessagingException("Failed to retrieve data", e);
		}
	}

	private byte[] firstLineFromNextFile() throws IOException {
		AbstractFileInfo<F> nextFile = poll();
		if (nextFile != null) {
			this.session = getRemoteFileTemplate().borrowSesssion();
			this.inputStream = this.session.readRaw(remotePath(nextFile));
			this.currentFile = nextFile;
			if (this.inputStream == null) {
				throw new MessagingException("No input stream for " + remotePath(nextFile));
			}
			return nextLine();
		}
		return null;
	}

	private byte[] nextLine() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		while (true) {
			int i = this.inputStream.read();
			if (checkEndOfFile(i)) {
				break;
			}
			if (i == this.lineDelimiter[0]) {
				if (this.lineDelimiter.length == 1) {
					if (this.includeDelimiters) {
						outputStream.write(i);
					}
					break;
				}
				int j = this.inputStream.read();
				if (checkEndOfFile(j)) {
					outputStream.write(i);
					break;
				}
				if (j == this.lineDelimiter[1]) {
					if (this.includeDelimiters) {
						outputStream.write(i);
						outputStream.write(j);
					}
					break;
				}
				else {
					outputStream.write(i);
					outputStream.write(j);
				}
			}
			else {
				outputStream.write(i);
			}
		}
		return outputStream.size() > 0 ? outputStream.toByteArray() : null;
	}

	private boolean checkEndOfFile(int i) throws IOException {
		if (i < 0) {
			this.session.finalizeRaw();
			releaseSession();
			return true;
		}
		else {
			return false;
		}
	}

	private void releaseSession() {
		getRemoteFileTemplate().returnSession(this.session);
		this.session = null;
	}

}
