/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.serializer;

import org.springframework.integration.ip.event.IpIntegrationEvent;

/**
 * Event representing an exception while decoding an incoming stream.
 * Contains the buffer of data decoded so far and the offset in the
 * buffer where the exception occurred, if available, otherwise -1.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public class TcpDeserializationExceptionEvent extends IpIntegrationEvent {

	private static final long serialVersionUID = 8812537718016054732L;

	private final byte[] buffer;

	private final int offset;

	public TcpDeserializationExceptionEvent(Object source, Throwable cause, byte[] buffer, //NOSONAR - direct storage
			int offset) {
		super(source, cause);
		this.buffer = buffer; //NOSONAR - direct storage
		this.offset = offset;
	}

	public byte[] getBuffer() {
		return this.buffer; //NOSONAR - direct access
	}

	public int getOffset() {
		return this.offset;
	}

}
