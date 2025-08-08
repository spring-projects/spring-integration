/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

/**
 * Base class for TCP Connection Support implementations.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public abstract class AbstractTcpConnectionSupport {

	private boolean pushbackCapable;

	private int pushbackBufferSize = 1;

	public boolean isPushbackCapable() {
		return this.pushbackCapable;
	}

	/**
	 * Set to true to cause wrapping of the connection's input stream in a
	 * {@link java.io.PushbackInputStream}, enabling deserializers to "unread" data.
	 * @param pushbackCapable true to enable.
	 */
	public void setPushbackCapable(boolean pushbackCapable) {
		this.pushbackCapable = pushbackCapable;
	}

	public int getPushbackBufferSize() {
		return this.pushbackBufferSize;
	}

	/**
	 * The size of the push back buffer; defaults to 1.
	 * @param pushbackBufferSize the size.
	 */
	public void setPushbackBufferSize(int pushbackBufferSize) {
		this.pushbackBufferSize = pushbackBufferSize;
	}

}
