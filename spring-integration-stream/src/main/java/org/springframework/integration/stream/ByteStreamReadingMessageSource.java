/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.stream;

import java.io.InputStream;

/**
 * A pollable source for receiving bytes from an {@link InputStream}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Ngoc Nhan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.stream.inbound.ByteStreamReadingMessageSource}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class ByteStreamReadingMessageSource
		extends org.springframework.integration.stream.inbound.ByteStreamReadingMessageSource {

	public ByteStreamReadingMessageSource(InputStream stream) {
		this(stream, -1);
	}

	public ByteStreamReadingMessageSource(InputStream stream, int bufferSize) {
		super(stream, bufferSize);
	}

}
