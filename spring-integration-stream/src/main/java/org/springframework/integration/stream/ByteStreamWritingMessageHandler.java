/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.stream;

import java.io.OutputStream;

/**
 * A {@link org.springframework.messaging.MessageHandler} that writes a byte array to an
 * {@link OutputStream}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Ngoc Nhan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.stream.outbound.ByteStreamWritingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class ByteStreamWritingMessageHandler extends
		org.springframework.integration.stream.outbound.ByteStreamWritingMessageHandler {

	public ByteStreamWritingMessageHandler(OutputStream stream) {
		this(stream, -1);
	}

	public ByteStreamWritingMessageHandler(OutputStream stream, int bufferSize) {
		super(stream, bufferSize);
	}

}
