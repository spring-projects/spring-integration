/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext // close at the end after class
public class StoredProcPollingChannelAdapterWithNamespace2IntegrationTests {

	@Autowired
	private AbstractApplicationContext context;

	@Autowired
	private Consumer consumer;

	@Test
	public void pollH2DatabaseUsingStoredProcedureCall() throws Exception {
		List<Message<List<Integer>>> received = new ArrayList<Message<List<Integer>>>();

		received.add(consumer.poll(60000));

		Message<List<Integer>> message = received.get(0);
		context.stop();
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();
		assertThat(message.getPayload() instanceof List<?>).isTrue();

		List<Integer> resultList = message.getPayload();

		assertThat(resultList.size() == 1).isTrue();

	}

	static class Counter {

		private final AtomicInteger count = new AtomicInteger();

		public Integer next() throws InterruptedException {
			if (count.get() > 2) {
				// prevent message overload
				return null;
			}
			return Integer.valueOf(count.incrementAndGet());
		}

	}

	static class Consumer {

		private final BlockingQueue<Message<List<Integer>>> messages = new LinkedBlockingQueue<Message<List<Integer>>>();

		@ServiceActivator
		public void receive(Message<List<Integer>> message) {
			messages.add(message);
		}

		Message<List<Integer>> poll(long timeoutInMillis) throws InterruptedException {
			return messages.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
		}

	}

}
