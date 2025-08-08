/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.stream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.messaging.MessagingException;

/**
 * A pollable source for receiving bytes from an {@link InputStream}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Christian Tzolov
 */
public class ByteStreamReadingMessageSource extends AbstractMessageSource<byte[]> {

	private final Lock lock = new ReentrantLock();

	private final BufferedInputStream stream;

	private int bytesPerMessage = 1024; // NOSONAR magic number

	private boolean shouldTruncate = true;

	public ByteStreamReadingMessageSource(InputStream stream) {
		this(stream, -1);
	}

	public ByteStreamReadingMessageSource(InputStream stream, int bufferSize) {
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

	@Override
	protected byte[] doReceive() {
		try {
			byte[] bytes;
			int bytesRead = 0;
			this.lock.lock();
			try {
				if (this.stream.available() == 0) {
					return null;
				}
				bytes = new byte[this.bytesPerMessage];
				bytesRead = this.stream.read(bytes, 0, bytes.length);
			}
			finally {
				this.lock.unlock();
			}
			if (bytesRead <= 0) {
				return null;
			}
			if (!this.shouldTruncate) {
				return bytes;
			}
			else {
				byte[] result = new byte[bytesRead];
				System.arraycopy(bytes, 0, result, 0, result.length);
				return result;
			}
		}
		catch (IOException e) {
			throw new MessagingException("IO failure occurred in adapter", e);
		}
	}

}
