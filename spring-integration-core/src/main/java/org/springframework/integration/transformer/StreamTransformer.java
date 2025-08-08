/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * Transforms an InputStream payload to a byte[] or String (if a
 * charset is provided).
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class StreamTransformer extends AbstractTransformer {

	private final String charset;

	/**
	 * Construct an instance to transform an {@link InputStream} to
	 * a {@code byte[]}.
	 */
	public StreamTransformer() {
		this(null);
	}

	/**
	 * Construct an instance with the charset to convert the stream to a
	 * String; if null a {@code byte[]} will be produced instead.
	 * @param charset the charset.
	 */
	public StreamTransformer(String charset) {
		this.charset = charset;
	}

	@Override
	protected Object doTransform(Message<?> message) {
		try {
			Assert.isTrue(message.getPayload() instanceof InputStream, "payload must be an InputStream");
			InputStream stream = (InputStream) message.getPayload();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			FileCopyUtils.copy(stream, baos);
			Closeable closeableResource = StaticMessageHeaderAccessor.getCloseableResource(message);
			if (closeableResource != null) {
				closeableResource.close();
			}
			return this.charset == null ? baos.toByteArray() : baos.toString(this.charset);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
