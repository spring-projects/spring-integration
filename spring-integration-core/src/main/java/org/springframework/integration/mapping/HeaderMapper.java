/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mapping;

import java.util.Map;

import org.springframework.messaging.MessageHeaders;

/**
 * Generic strategy interface for mapping {@link MessageHeaders} to and from other
 * types of objects. This would typically be used by adapters where the "other type"
 * has a concept of headers or properties (HTTP, JMS, AMQP, etc).
 *
 * @param <T> type of the instance to and from which headers will be mapped.
 *
 * @author Mark Fisher
 */
public interface HeaderMapper<T> {

	void fromHeaders(MessageHeaders headers, T target);

	Map<String, Object> toHeaders(T source);

}
