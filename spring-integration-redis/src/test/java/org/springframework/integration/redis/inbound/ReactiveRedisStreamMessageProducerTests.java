/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.integration.redis.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.integration.redis.outbound.ReactiveRedisStreamMessageHandler;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableRule;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.redis.util.Address;
import org.springframework.integration.redis.util.Person;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Attoumane Ahamadi
 * @author Artem Bilan
 * @author Rohan Mukesh
 *
 * @since 5.4
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ReactiveRedisStreamMessageProducerTests extends RedisAvailableTests {

	private static final String STREAM_KEY = ReactiveRedisStreamMessageProducerTests.class.getSimpleName() + ".stream";

	private static final String CONSUMER = ReactiveRedisStreamMessageProducerTests.class.getSimpleName() + ".consumer";

	@Autowired
	FluxMessageChannel fluxMessageChannel;

	@Autowired
	ReactiveRedisStreamMessageProducer reactiveRedisStreamProducer;

	@Autowired
	ReactiveRedisTemplate<String, ?> template;

	@Autowired
	ReactiveMessageHandlerAdapter messageHandler;

	@Before
	public void delKey() {
		this.template.delete(STREAM_KEY).block();
	}

	@After
	public void tearDown() {
		this.reactiveRedisStreamProducer.stop();
	}

	@Test
	@RedisAvailable
	public void testConsumerGroupCreation() {
		this.reactiveRedisStreamProducer.setCreateConsumerGroup(true);
		this.reactiveRedisStreamProducer.setConsumerName(CONSUMER);
		this.reactiveRedisStreamProducer.afterPropertiesSet();

		Flux.from(this.fluxMessageChannel).subscribe();

		this.reactiveRedisStreamProducer.start();

		this.template.opsForStream()
				.groups(STREAM_KEY)
				.next()
				.as(StepVerifier::create)
				.assertNext((infoGroup) ->
						assertThat(infoGroup.groupName()).isEqualTo(this.reactiveRedisStreamProducer.getBeanName()))
				.thenCancel()
				.verify(Duration.ofSeconds(10));
	}

	@Test
	@RedisAvailable
	public void testReadingMessageAsStandaloneClient() {
		Address address = new Address("Rennes 3, France");
		Person person = new Person(address, "Attoumane");
		this.messageHandler.handleMessage(new GenericMessage<>(person));

		this.reactiveRedisStreamProducer.setCreateConsumerGroup(false);
		this.reactiveRedisStreamProducer.setConsumerName(null);
		this.reactiveRedisStreamProducer.setReadOffset(ReadOffset.from("0-0"));
		this.reactiveRedisStreamProducer.afterPropertiesSet();

		StepVerifier stepVerifier =
				Flux.from(this.fluxMessageChannel)
						.as(StepVerifier::create)
						.assertNext(message -> {
							assertThat(message.getPayload()).isEqualTo(person);
							assertThat(message.getHeaders()).containsKeys(RedisHeaders.STREAM_KEY,
									RedisHeaders.STREAM_MESSAGE_ID);
						})
						.thenCancel()
						.verifyLater();

		this.reactiveRedisStreamProducer.start();

		stepVerifier.verify(Duration.ofSeconds(10));
	}

	@Test
	@RedisAvailable
	public void testReadingMessageAsConsumerInConsumerGroup() {
		Address address = new Address("Winterfell, Westeros");
		Person person = new Person(address, "John Snow");

		this.template.opsForStream()
				.createGroup(STREAM_KEY, this.reactiveRedisStreamProducer.getBeanName())
				.as(StepVerifier::create)
				.assertNext(message -> assertThat(message).isEqualTo("OK"))
				.thenCancel()
				.verify(Duration.ofSeconds(10));

		this.reactiveRedisStreamProducer.setCreateConsumerGroup(false);
		this.reactiveRedisStreamProducer.setConsumerName(CONSUMER);
		this.reactiveRedisStreamProducer.setReadOffset(ReadOffset.latest());
		this.reactiveRedisStreamProducer.afterPropertiesSet();
		this.reactiveRedisStreamProducer.start();

		StepVerifier stepVerifier =
				Flux.from(this.fluxMessageChannel)
						.as(StepVerifier::create)
						.assertNext(message -> {
							assertThat(message.getPayload()).isEqualTo(person);
							assertThat(message.getHeaders()).containsKeys(RedisHeaders.CONSUMER_GROUP, RedisHeaders.CONSUMER);
						})
						.thenCancel()
						.verifyLater();

		this.messageHandler.handleMessage(new GenericMessage<>(person));

		stepVerifier.verify(Duration.ofSeconds(10));
	}

	@Test
	@RedisAvailable
	public void testReadingPendingMessageWithNoAutoACK() {
		Address address = new Address("Winterfell, Westeros");
		Person person = new Person(address, "John Snow");

		String consumerGroup = "testGroup";
		String consumerName = "testConsumer";

		this.reactiveRedisStreamProducer.setCreateConsumerGroup(true);
		this.reactiveRedisStreamProducer.setAutoAck(false);
		this.reactiveRedisStreamProducer.setConsumerGroup(consumerGroup);
		this.reactiveRedisStreamProducer.setConsumerName(consumerName);
		this.reactiveRedisStreamProducer.setReadOffset(ReadOffset.latest());
		this.reactiveRedisStreamProducer.afterPropertiesSet();
		this.reactiveRedisStreamProducer.start();

		AtomicReference<SimpleAcknowledgment> acknowledgmentReference = new AtomicReference<>();

		StepVerifier stepVerifier =
				Flux.from(this.fluxMessageChannel)
						.as(StepVerifier::create)
						.assertNext(message -> {
							assertThat(message.getPayload()).isEqualTo(person);
							assertThat(message.getHeaders())
									.containsKeys(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK);
							acknowledgmentReference.set(StaticMessageHeaderAccessor.getAcknowledgment(message));
						})
						.thenCancel()
						.verifyLater();

		this.messageHandler.handleMessage(new GenericMessage<>(person));

		stepVerifier.verify(Duration.ofSeconds(10));

		await().until(() ->
				template.opsForStream()
						.pending(STREAM_KEY, consumerGroup)
						.block(Duration.ofMillis(100))
						.getTotalPendingMessages() == 1);

		acknowledgmentReference.get().acknowledge();

		Mono<PendingMessagesSummary> pendingZeroMessage =
				template.opsForStream().pending(STREAM_KEY, consumerGroup);

		StepVerifier.create(pendingZeroMessage)
				.assertNext(pendingMessagesSummary ->
						assertThat(pendingMessagesSummary.getTotalPendingMessages()).isEqualTo(0))
				.verifyComplete();
	}

	@Autowired
	ReactiveRedisStreamMessageProducer reactiveErrorRedisStreamProducer;

	@Autowired
	PollableChannel redisStreamErrorChannel;

	@Test
	@RedisAvailable
	public void testReadingNextMessagesWhenSerializationException() {
		Person person = new Person(new Address("Winterfell, Westeros"), "John Snow");
		Date testDate = new Date();
		this.reactiveErrorRedisStreamProducer.start();

		StepVerifier stepVerifier =
				Flux.from(this.fluxMessageChannel)
						.map(Message::getPayload)
						.cast(Date.class)
						.as(StepVerifier::create)
						.expectNext(testDate)
						.thenCancel()
						.verifyLater();

		this.messageHandler.handleMessage(new GenericMessage<>(person));

		Message<?> errorMessage = this.redisStreamErrorChannel.receive(10_000);
		assertThat(errorMessage).isInstanceOf(ErrorMessage.class)
				.extracting("payload.message")
				.asInstanceOf(InstanceOfAssertFactories.STRING)
				.contains("Cannot deserialize Redis Stream Record")
				.contains("Cannot parse date out of");

		Mono<PendingMessagesSummary> pendingMessage =
				template.opsForStream()
						.pending(STREAM_KEY, this.reactiveErrorRedisStreamProducer.getBeanName());

		StepVerifier.create(pendingMessage)
				.assertNext(pendingMessagesSummary ->
						assertThat(pendingMessagesSummary.getTotalPendingMessages()).isEqualTo(1L))
				.verifyComplete();

		Message<?> failedMessage = ((MessagingException) errorMessage.getPayload()).getFailedMessage();
		StaticMessageHeaderAccessor.getAcknowledgment(failedMessage).acknowledge();

		pendingMessage =
				template.opsForStream()
						.pending(STREAM_KEY, this.reactiveErrorRedisStreamProducer.getBeanName());

		StepVerifier.create(pendingMessage)
				.assertNext(pendingMessagesSummary ->
						assertThat(pendingMessagesSummary.getTotalPendingMessages()).isEqualTo(0))
				.verifyComplete();

		this.messageHandler.handleMessage(new GenericMessage<>(testDate));

		stepVerifier.verify(Duration.ofSeconds(10));

		this.reactiveErrorRedisStreamProducer.stop();
	}

	@Configuration
	static class ContextConfig {

		@Bean
		ReactiveRedisStreamMessageHandler redisStreamMessageHandler() {
			return new ReactiveRedisStreamMessageHandler(RedisAvailableRule.connectionFactory, STREAM_KEY);
		}

		@Bean
		public ReactiveMessageHandlerAdapter reactiveMessageHandlerAdapter() {
			return new ReactiveMessageHandlerAdapter(redisStreamMessageHandler());
		}

		@Bean
		ReactiveRedisTemplate<String, ?> reactiveStreamOperations() {
			return new ReactiveRedisTemplate<>(RedisAvailableRule.connectionFactory,
					RedisSerializationContext.string());
		}

		@Bean
		FluxMessageChannel fluxMessageChannel() {
			return new FluxMessageChannel();
		}

		@Bean
		PollableChannel redisStreamErrorChannel() {
			return new QueueChannel();
		}

		@Bean
		ReactiveRedisStreamMessageProducer reactiveErrorRedisStreamProducer() {
			ReactiveRedisStreamMessageProducer messageProducer =
					new ReactiveRedisStreamMessageProducer(RedisAvailableRule.connectionFactory, STREAM_KEY);
			messageProducer.setTargetType(Date.class);
			messageProducer.setPollTimeout(Duration.ofMillis(100));
			messageProducer.setCreateConsumerGroup(true);
			messageProducer.setAutoAck(false);
			messageProducer.setConsumerName("testConsumer");
			messageProducer.setReadOffset(ReadOffset.latest());
			messageProducer.setAutoStartup(false);
			messageProducer.setOutputChannel(fluxMessageChannel());
			messageProducer.setErrorChannel(redisStreamErrorChannel());
			return messageProducer;
		}

		@Bean
		ReactiveRedisStreamMessageProducer reactiveRedisStreamProducer() {
			ReactiveRedisStreamMessageProducer messageProducer =
					new ReactiveRedisStreamMessageProducer(RedisAvailableRule.connectionFactory, STREAM_KEY);
			messageProducer.setStreamReceiverOptions(
					StreamReceiver.StreamReceiverOptions.builder()
							.pollTimeout(Duration.ofMillis(100))
							.targetType(Person.class)
							.build());
			messageProducer.setAutoStartup(false);
			messageProducer.setOutputChannel(fluxMessageChannel());
			return messageProducer;
		}

	}

}
