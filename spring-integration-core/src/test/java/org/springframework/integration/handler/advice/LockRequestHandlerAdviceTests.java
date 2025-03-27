/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.integration.handler.advice;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.AsyncMessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 6.5
 */
@SpringJUnitConfig
@DirtiesContext
public class LockRequestHandlerAdviceTests {

	private static final String LOCK_KEY_HEADER = "lock-key-header";

	@Autowired
	MessageChannel inputChannel;

	@Autowired
	QueueChannel discardChannel;

	@Test
	void verifyLockAroundHandler() throws ExecutionException, InterruptedException, TimeoutException {
		AsyncMessagingTemplate messagingTemplate = new AsyncMessagingTemplate();
		MessagePostProcessor messagePostProcessor =
				message ->
						MessageBuilder.fromMessage(message)
								.setHeader(LOCK_KEY_HEADER, "someLock")
								.build();
		Future<Object> test1 =
				messagingTemplate.asyncConvertSendAndReceive(this.inputChannel, "test1", messagePostProcessor);
		Future<Object> test2 =
				messagingTemplate.asyncConvertSendAndReceive(this.inputChannel, "test2", messagePostProcessor);

		assertThat(test1.get(10, TimeUnit.SECONDS)).isEqualTo("test1-1");
		assertThat(test2.get(10, TimeUnit.SECONDS)).isEqualTo("test2-1");

		messagingTemplate.send(this.inputChannel, new GenericMessage<>("no_lock_key"));

		Message<?> receive = this.discardChannel.receive(10_000);
		assertThat(receive)
				.extracting(Message::getPayload)
				.isEqualTo("no_lock_key");

		Future<Object> test3 =
				messagingTemplate.asyncConvertSendAndReceive(this.inputChannel, "longer_process", messagePostProcessor);

		Future<Object> test4 =
				messagingTemplate.asyncConvertSendAndReceive(this.inputChannel, "test4", messagePostProcessor);

		assertThat(test3.get(10, TimeUnit.SECONDS)).isEqualTo("longer_process-1");

		assertThat(test4).failsWithin(10, TimeUnit.SECONDS)
				.withThrowableOfType(ExecutionException.class)
				.withRootCauseInstanceOf(TimeoutException.class)
				.withStackTraceContaining("Could not acquire the lock in time: PT1S");
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		LockRegistry lockRegistry() {
			return new DefaultLockRegistry();
		}

		@Bean
		QueueChannel discardChannel() {
			return new QueueChannel();
		}

		@Bean
		LockRequestHandlerAdvice lockRequestHandlerAdvice(LockRegistry lockRegistry, QueueChannel discardChannel) {
			LockRequestHandlerAdvice lockRequestHandlerAdvice =
					new LockRequestHandlerAdvice(lockRegistry, (message) -> message.getHeaders().get(LOCK_KEY_HEADER));
			lockRequestHandlerAdvice.setDiscardChannel(discardChannel);
			lockRequestHandlerAdvice.setWaitLockDurationExpressionString("'PT1s'");
			return lockRequestHandlerAdvice;
		}

		AtomicInteger counter = new AtomicInteger();

		@ServiceActivator(inputChannel = "inputChannel", adviceChain = "lockRequestHandlerAdvice")
		String handleWithDelay(String payload) throws InterruptedException {
			int currentCount = this.counter.incrementAndGet();
			Thread.sleep("longer_process".equals(payload) ? 5000 : 500);
			try {
				return payload + "-" + currentCount;
			}
			finally {
				this.counter.decrementAndGet();
			}
		}

	}

}
