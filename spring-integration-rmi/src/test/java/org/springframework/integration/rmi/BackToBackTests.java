/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.integration.rmi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class BackToBackTests {

	@Autowired
	private SubscribableChannel good;

	@Autowired
	private SubscribableChannel bad;

	@Autowired
	private SubscribableChannel ugly;

	@Autowired
	private PollableChannel reply;

	@Autowired
	private AbstractApplicationContext context;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	public void testGood() {
		good.send(new GenericMessage<>("foo"));
		Message<?> reply = this.reply.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("reply:foo");

		verify(this.transactionManager).getTransaction(any(TransactionDefinition.class));
	}

	@Test
	public void testBad() {
		bad.send(new GenericMessage<>("foo"));
		Message<?> reply = this.reply.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("error:foo");
	}

	@Test
	public void testUgly() {
		context.setId("context");
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> ugly.send(new GenericMessage<>("foo")))
				.withCauseInstanceOf(MessageDeliveryException.class)
				.withRootCauseInstanceOf(MessageDispatchingException.class)
				.withMessageContaining("Dispatcher has no subscribers for channel 'context.baz'.");
	}

}
