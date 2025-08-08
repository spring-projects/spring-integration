/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mapping;

import java.util.Map;

import org.springframework.messaging.MessageHeaders;

/**
 * Request/Reply strategy interface for mapping {@link MessageHeaders} to and from other
 * types of objects. This would typically be used by adapters where the "other type"
 * has a concept of headers or properties (HTTP, JMS, AMQP, etc).
 *
 * @param <T> the type of the target object holding the headers
 * @author Oleg Zhurakousky
 * @author Stephane Nicoll
 * @since 2.1
 */
public interface RequestReplyHeaderMapper<T> {

	/**
	 * Map from the given {@link MessageHeaders} to the specified request target.
	 * @param headers the abstracted MessageHeaders
	 * @param target the native target request
	 */
	void fromHeadersToRequest(MessageHeaders headers, T target);

	/**
	 * Map from the given {@link MessageHeaders} to the specified reply target.
	 * @param headers the abstracted MessageHeaders
	 * @param target the native target reply
	 */
	void fromHeadersToReply(MessageHeaders headers, T target);

	/**
	 * Map from the given request object to abstracted {@link MessageHeaders}.
	 * @param source the native target request
	 * @return the abstracted MessageHeaders
	 */
	Map<String, Object> toHeadersFromRequest(T source);

	/**
	 * Map from the given reply object to abstracted {@link MessageHeaders}.
	 * @param source the native target reply
	 * @return the abstracted MessageHeaders
	 */
	Map<String, Object> toHeadersFromReply(T source);

}
