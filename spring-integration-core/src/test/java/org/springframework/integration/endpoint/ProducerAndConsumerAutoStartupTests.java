/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class ProducerAndConsumerAutoStartupTests {

	@Autowired
	private AbstractApplicationContext context;

	@Autowired
	private Consumer consumer;

	@Test
	public void test() throws Exception {
		List<Integer> received = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			received.add(this.consumer.poll(10000));
		}
		this.context.stop();
		assertThat(received.get(0)).isEqualTo(1);
		assertThat(received.get(1)).isEqualTo(2);
		assertThat(received.get(2)).isEqualTo(3);
	}

	static class Counter {

		private final AtomicInteger count = new AtomicInteger();

		public Integer next() {
			if (this.count.get() > 2) {
				//prevent message overload
				return null;
			}
			return this.count.incrementAndGet();
		}

	}

	static class Consumer {

		private final BlockingQueue<Integer> numbers = new LinkedBlockingQueue<>();

		public void receive(Integer number) {
			this.numbers.add(number);
		}

		Integer poll(long timeoutInMillis) throws InterruptedException {
			return this.numbers.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
		}

	}

}
