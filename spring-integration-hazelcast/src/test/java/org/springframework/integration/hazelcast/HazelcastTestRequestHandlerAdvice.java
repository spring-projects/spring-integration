/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast;

import java.util.concurrent.CountDownLatch;

import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.Message;

/**
 * {@link AbstractRequestHandlerAdvice} advice class for Hazelcast Integration Unit Tests.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class HazelcastTestRequestHandlerAdvice extends AbstractRequestHandlerAdvice {

	public CountDownLatch executeLatch = null;

	public HazelcastTestRequestHandlerAdvice(int count) {
		this.executeLatch = new CountDownLatch(count);
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		try {
			return callback.execute();
		}
		finally {
			this.executeLatch.countDown();
		}
	}

}
