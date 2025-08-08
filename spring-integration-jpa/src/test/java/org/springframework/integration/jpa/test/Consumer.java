/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.test;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;

/**
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public final class Consumer {

	private static final Log logger = LogFactory.getLog(Consumer.class);

	private static final BlockingQueue<Message<Collection<?>>> MESSAGES = new LinkedBlockingQueue<Message<Collection<?>>>();

	public synchronized void receive(Message<Collection<?>> message) {
		logger.info("Service Activator received Message: " + message);
		MESSAGES.add(message);
	}

	public synchronized Message<Collection<?>> poll(long timeoutInMillis) throws InterruptedException {
		return MESSAGES.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
	}

}
