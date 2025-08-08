/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.mapping;

import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * An {@link OutboundMessageMapper} and {@link InboundMessageMapper} that
 * maps to/from {@code byte[]}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public interface BytesMessageMapper extends InboundMessageMapper<byte[]>, OutboundMessageMapper<byte[]> {

	@Override
	@NonNull // override
	default Message<?> toMessage(byte[] object) {
		return toMessage(object, null);
	}

	@Override
	@NonNull
		// override
	Message<?> toMessage(byte[] bytes, @Nullable Map<String, Object> headers);

}
