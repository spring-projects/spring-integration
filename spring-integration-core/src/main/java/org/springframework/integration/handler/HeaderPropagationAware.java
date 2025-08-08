/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.handler;

import java.util.Collection;

/**
 * MessageHandlers implementing this interface can propagate headers from
 * an input message to an output message.
 *
 * @author Gary Russell
 * @since 4.3.11
 *
 */
public interface HeaderPropagationAware {

	/**
	 * Set headers that will NOT be copied from the inbound message if
	 * the handler is configured to copy headers.
	 * @param headers the headers to not propagate from the inbound message.
	 */
	void setNotPropagatedHeaders(String... headers);

	/**
	 * Get the header names this handler doesn't propagate.
	 * @return an immutable {@link java.util.Collection} of headers that will not be
	 * copied from the inbound message if the handler is configured to copy headers.
	 * @see #setNotPropagatedHeaders(String...)
	 */
	Collection<String> getNotPropagatedHeaders();

	/**
	 * Add headers that will NOT be copied from the inbound message if
	 * the handler is configured to copy headers, instead of overwriting
	 * the existing set.
	 * @param headers the headers to not propagate from the inbound message.
	 * @see #setNotPropagatedHeaders(String...)
	 */
	void addNotPropagatedHeaders(String... headers);

}
