/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.stream.outbound;

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
 * @author Artem Bilan
 *
 * @since 7.0
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
