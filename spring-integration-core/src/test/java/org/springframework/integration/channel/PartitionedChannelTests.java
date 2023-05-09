/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Artem Bilan
 *
 * @since 6.1
 */
public class PartitionedChannelTests {

	@Test
	void messagesAreProperlyPartitioned() throws InterruptedException {
		PartitionedChannel partitionedChannel =
				new PartitionedChannel(2, (message) -> message.getHeaders().get("partitionKey"));
		partitionedChannel.setBeanFactory(mock(BeanFactory.class));
		partitionedChannel.setBeanName("testPartitionedChannel");

		CountDownLatch handleLatch = new CountDownLatch(4);

		partitionedChannel.addInterceptor(new ExecutorChannelInterceptor() {

			@Override
			public void afterMessageHandled(Message<?> message, MessageChannel ch, MessageHandler h, Exception ex) {
				handleLatch.countDown();
			}

		});
		partitionedChannel.afterPropertiesSet();

		MultiValueMap<String, Message<?>> partitionedMessages = new LinkedMultiValueMap<>();

		partitionedChannel.subscribe((message) -> partitionedMessages.add(Thread.currentThread().getName(), message));

		partitionedChannel.send(MessageBuilder.withPayload("test1").setHeader("partitionKey", "1").build());
		partitionedChannel.send(MessageBuilder.withPayload("test2").setHeader("partitionKey", "2").build());
		partitionedChannel.send(MessageBuilder.withPayload("test3").setHeader("partitionKey", "2").build());
		partitionedChannel.send(MessageBuilder.withPayload("test4").setHeader("partitionKey", "1").build());

		assertThat(handleLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(partitionedMessages).hasSize(2);
		partitionedMessages.values()
				.forEach(messagesInPartition -> {
					assertThat(messagesInPartition).hasSize(2);
					assertThat(messagesInPartition.get(0).getHeaders().get("partitionKey"))
							.isEqualTo(messagesInPartition.get(1).getHeaders().get("partitionKey"));
				});


		HashSet<String> allocatedPartitions = new HashSet<>(partitionedMessages.keySet());
		partitionedMessages.clear();

		CountDownLatch anotherHandleLatch = new CountDownLatch(1);

		partitionedChannel.addInterceptor(new ExecutorChannelInterceptor() {

			@Override
			public void afterMessageHandled(Message<?> message, MessageChannel ch, MessageHandler h, Exception ex) {
				anotherHandleLatch.countDown();
			}

		});

		partitionedChannel.send(MessageBuilder.withPayload("test4").setHeader("partitionKey", "3").build());

		assertThat(anotherHandleLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(partitionedMessages).hasSize(1);
		String partitionForLastMessage = partitionedMessages.keySet().iterator().next();
		assertThat(partitionForLastMessage).isIn(allocatedPartitions);

		partitionedChannel.destroy();
	}

}
