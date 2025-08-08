/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import org.springframework.messaging.MessageHandler;

/**
 * Operations to perform on some message source.
 *
 * @author Gary Russell
 *
 * @since 5.0.1
 *
 */
public interface PollingOperations {

	/**
	 * Poll for a message.
	 * @param handler a message handler.
	 * @return the message
	 */
	boolean poll(MessageHandler handler);

}
