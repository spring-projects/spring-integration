/*
 * Copyright 2002-2014 the original author or authors.
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.support.GenericMessage;

/**
 * A pollable source for receiving bytes from an {@link InputStream}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class ByteStreamReadingMessageSource extends IntegrationObjectSupport implements MessageSource<byte[]> {

	private BufferedInputStream stream;

	private Object streamMonitor;

	private int bytesPerMessage = 1024;

	private boolean shouldTruncate = true;


	public ByteStreamReadingMessageSource(InputStream stream) {
		this(stream, -1);
	}

	public ByteStreamReadingMessageSource(InputStream stream, int bufferSize) {
		this.streamMonitor = stream;
		if (stream instanceof BufferedInputStream) {
			this.stream = (BufferedInputStream) stream;
		}
		else if (bufferSize > 0) {
			this.stream = new BufferedInputStream(stream, bufferSize);
		}
		else {
			this.stream = new BufferedInputStream(stream);
		}
	}


	public void setBytesPerMessage(int bytesPerMessage) {
		this.bytesPerMessage = bytesPerMessage;
	}

	public void setShouldTruncate(boolean shouldTruncate) {
		this.shouldTruncate = shouldTruncate;
	}

	@Override
	public String getComponentType() {
		return "stream:stdin-channel-adapter(byte)";
	}

	public Message<byte[]> receive() {
		try {
			byte[] bytes;
			int bytesRead = 0;
			synchronized (this.streamMonitor) {
				if (stream.available() == 0) {
					return null;
				}
				bytes = new byte[bytesPerMessage];
				bytesRead = stream.read(bytes, 0, bytes.length);
			}
			if (bytesRead <= 0) {
				return null;
			}
			if (!this.shouldTruncate) {
				return new GenericMessage<byte[]>(bytes);
			}
			else {
				byte[] result = new byte[bytesRead];
				System.arraycopy(bytes, 0, result, 0, result.length);
				return new GenericMessage<byte[]>(result);
			}
		}
		catch (IOException e) {
			throw new MessagingException("IO failure occurred in adapter", e);
		}
	}

}
