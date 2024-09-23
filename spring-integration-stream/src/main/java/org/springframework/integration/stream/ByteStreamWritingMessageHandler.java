/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.stream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * A {@link org.springframework.messaging.MessageHandler} that writes a byte array to an
 * {@link OutputStream}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Ngoc Nhan
 */
public class ByteStreamWritingMessageHandler extends AbstractMessageHandler {

	private final BufferedOutputStream stream;

	public ByteStreamWritingMessageHandler(OutputStream stream) {
		this(stream, -1);
	}

	public ByteStreamWritingMessageHandler(OutputStream stream, int bufferSize) {
		if (bufferSize > 0) {
			this.stream = new BufferedOutputStream(stream, bufferSize);
		}
		else {
			this.stream = new BufferedOutputStream(stream);
		}
	}

	@Override
	public String getComponentType() {
		return "stream:outbound-channel-adapter(byte)";
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Object payload = message.getPayload();
		try {
			if (payload instanceof String string) {
				this.stream.write(string.getBytes());
			}
			else if (payload instanceof byte[] bytes) {
				this.stream.write(bytes);
			}
			else {
				throw new MessagingException(this.getClass().getSimpleName() +
						" only supports byte array and String-based messages");
			}
			this.stream.flush();
		}
		catch (IOException e) {
			throw new MessagingException("IO failure occurred in target", e);
		}
	}

}
