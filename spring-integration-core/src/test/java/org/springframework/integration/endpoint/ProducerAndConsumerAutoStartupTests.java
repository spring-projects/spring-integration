/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Artem Bilan
 *
 * @since 2.0
 */
@RunWith(SpringRunner.class)
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
