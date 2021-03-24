/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
@SpringJUnitConfig
@DirtiesContext
public class DelayerUsageTests {

	@Autowired
	@Qualifier("inputA")
	private MessageChannel inputA;

	@Autowired
	private MessageChannel delayerInsideChain;

	@Autowired
	@Qualifier("outputA")
	private PollableChannel outputA;

	@Autowired
	@Qualifier("inputB")
	private MessageChannel inputB;

	@Autowired
	@Qualifier("outputB1")
	private PollableChannel outputB1;

	@Autowired
	private MessageChannel inputC;

	@Autowired
	private PollableChannel outputC;


	@Test
	public void testDelayWithDefaultSchedulerAndTransactionSynchronization() {
		long start = System.currentTimeMillis();

		new TransactionTemplate(new PseudoTransactionManager())
				.execute(status -> {
					inputA.send(new GenericMessage<>("Hello"));
					return null;
				});

		assertThat(outputA.receive(10000)).isNotNull();
		assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(1000);
	}

	@Test
	public void testDelayWithDefaultSchedulerCustomDelayHeader() {
		MessageBuilder<String> builder = MessageBuilder.withPayload("Hello");
		// set custom delay header
		builder.setHeader("foo", 2000);
		long start = System.currentTimeMillis();
		inputA.send(builder.build());
		assertThat(outputA.receive(10000)).isNotNull();
		assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(2000);
	}

	@Test
	public void testDelayWithCustomScheduler() {
		long start = System.currentTimeMillis();
		inputB.send(new GenericMessage<>("1"));
		inputB.send(new GenericMessage<>("2"));
		inputB.send(new GenericMessage<>("3"));
		inputB.send(new GenericMessage<>("4"));
		inputB.send(new GenericMessage<>("5"));
		inputB.send(new GenericMessage<>("6"));
		inputB.send(new GenericMessage<>("7"));
		assertThat(outputB1.receive(10000)).isNotNull();
		assertThat(outputB1.receive(10000)).isNotNull();
		assertThat(outputB1.receive(10000)).isNotNull();
		assertThat(outputB1.receive(10000)).isNotNull();
		assertThat(outputB1.receive(10000)).isNotNull();
		assertThat(outputB1.receive(10000)).isNotNull();
		assertThat(outputB1.receive(10000)).isNotNull();

		// must execute under 3 seconds, since threadPool is set too 5.
		// first batch is 5 concurrent invocations on SA, then 2 more
		// elapsed time for the whole execution should be a bit over 2 seconds depending on the hardware
		assertThat(System.currentTimeMillis() - start).isBetween(1000L, 3000L);
	}

	@Test
	public void testDelayerInsideChain() {
		long start = System.currentTimeMillis();
		delayerInsideChain.send(new GenericMessage<>("Hello"));
		Message<?> message = outputA.receive(10000);
		assertThat(message).isNotNull();
		assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(1000);
		assertThat(message.getPayload()).isEqualTo("hello");
	}

	@Test
	public void testInt2243DelayerExpression() {
		long start = System.currentTimeMillis();
		this.inputC.send(new GenericMessage<>("test"));
		Message<?> message = this.outputC.receive(10000);
		assertThat(message).isNotNull();
		assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(1000);
		assertThat(message.getPayload()).isEqualTo("test");
	}

	public static class SampleService {

		public String processMessage(String message) throws Exception {
			Thread.sleep(500);
			return message;
		}

	}

}
