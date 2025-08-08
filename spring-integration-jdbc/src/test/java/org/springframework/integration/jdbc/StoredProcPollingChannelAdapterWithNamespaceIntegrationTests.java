/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class StoredProcPollingChannelAdapterWithNamespaceIntegrationTests {

	@Autowired
	private AbstractApplicationContext context;

	@Autowired
	private Consumer consumer;

	@SuppressWarnings("unchecked")
	@Test
	public void pollH2DatabaseUsingStoredProcedureCall() throws Exception {
		List<Message<?>> received = new ArrayList<>();

		received.add(consumer.poll(60000));

		Message<?> message = received.get(0);
		context.stop();
		assertThat(message).isNotNull();
		assertThat(message.getPayload())
				.isNotNull()
				.isInstanceOf(Collection.class);

		List<Integer> primeNumbers = (List<Integer>) message.getPayload();

		assertThat(primeNumbers).containsExactly(2, 3, 5, 7);

	}

	static class Counter {

		private final AtomicInteger count = new AtomicInteger();

		public Integer next() throws InterruptedException {
			if (count.get() > 2) {
				// prevent message overload
				return null;
			}
			return count.incrementAndGet();
		}

	}

	static class Consumer {

		private final BlockingQueue<Message<?>> messages = new LinkedBlockingQueue<>();

		@ServiceActivator
		public void receive(Message<?> message) {
			messages.add(message);
		}

		Message<?> poll(long timeoutInMillis) throws InterruptedException {
			return messages.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
		}

	}

}
