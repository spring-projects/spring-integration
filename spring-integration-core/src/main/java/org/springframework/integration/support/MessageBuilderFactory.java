/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.support;

import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
public interface MessageBuilderFactory {

	<T> AbstractIntegrationMessageBuilder<T> fromMessage(Message<T> message);

	<T> AbstractIntegrationMessageBuilder<T> withPayload(T payload);

}
