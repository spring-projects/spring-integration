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

package org.springframework.integration.redis.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.events.IntegrationEvent;
import org.springframework.integration.redis.event.RedisExceptionEvent;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ClassUtils;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @author Rainer Frey
 *
 * @since 3.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class RedisQueueMessageDrivenEndpointTests extends RedisAvailableTests {

	public static final String TEST_QUEUE = UUID.randomUUID().toString();

	@Autowired
	private RedisConnectionFactory connectionFactory;

	@Autowired
	private PollableChannel fromChannel;

	@Autowired
	private SmartLifecycle fromChannelEndpoint;

	@Autowired
	private MessageChannel symmetricalInputChannel;

	@Autowired
	private SmartLifecycle symmetricalRedisChannelEndpoint;

	@Autowired
	private PollableChannel symmetricalOutputChannel;

	@Before
	public void setUpTearDown() {
		RedisTemplate<String, ?> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.afterPropertiesSet();
		redisTemplate.delete(TEST_QUEUE);
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testInt3014Default() throws InterruptedException {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		String payload = "testing";

		redisTemplate.boundListOps(TEST_QUEUE).leftPush(payload);

		Date payload2 = new Date();

		redisTemplate.boundListOps(TEST_QUEUE).leftPush(payload2);

		PollableChannel channel = new QueueChannel();

		RedisQueueMessageDrivenEndpoint endpoint =
				new RedisQueueMessageDrivenEndpoint(TEST_QUEUE, this.connectionFactory);
		endpoint.setBeanFactory(Mockito.mock(BeanFactory.class));
		endpoint.setBeanClassLoader(ClassUtils.getDefaultClassLoader());
		endpoint.setOutputChannel(channel);
		endpoint.setReceiveTimeout(10);
		endpoint.afterPropertiesSet();
		endpoint.start();

		Message<Object> receive = (Message<Object>) channel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(payload);

		receive = (Message<Object>) channel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(payload2);

		CountDownLatch stopLatch = new CountDownLatch(1);
		endpoint.stop(stopLatch::countDown);
		assertThat(stopLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testInt3014ExpectMessageTrue() throws InterruptedException {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		Message<?> message = MessageBuilder.withPayload("testing").build();

		redisTemplate.boundListOps(TEST_QUEUE).leftPush(message);

		redisTemplate.boundListOps(TEST_QUEUE).leftPush("test");

		PollableChannel channel = new QueueChannel();

		PollableChannel errorChannel = new QueueChannel();

		RedisQueueMessageDrivenEndpoint endpoint =
				new RedisQueueMessageDrivenEndpoint(TEST_QUEUE, this.connectionFactory);
		endpoint.setBeanFactory(Mockito.mock(BeanFactory.class));
		endpoint.setExpectMessage(true);
		endpoint.setSerializer(new JdkSerializationRedisSerializer());
		endpoint.setOutputChannel(channel);
		endpoint.setErrorChannel(errorChannel);
		endpoint.setReceiveTimeout(10);
		endpoint.afterPropertiesSet();
		endpoint.start();

		Message<Object> receive = (Message<Object>) channel.receive(10000);
		assertThat(receive).isNotNull();

		assertThat(receive).isEqualTo(message);

		receive = (Message<Object>) errorChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive).isInstanceOf(ErrorMessage.class);
		assertThat(receive.getPayload()).isInstanceOf(MessagingException.class);
		assertThat(((Exception) receive.getPayload()).getMessage()).contains("Deserialization of Message failed.");
		assertThat(((Exception) receive.getPayload()).getCause()).isInstanceOf(ClassCastException.class);
		assertThat(((Exception) receive.getPayload()).getCause().getMessage())
				.contains("java.lang.String cannot be cast");

		CountDownLatch stopLatch = new CountDownLatch(1);
		endpoint.stop(stopLatch::countDown);
		assertThat(stopLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	@RedisAvailable
	public void testInt3017IntegrationInbound() throws InterruptedException {
		this.fromChannelEndpoint.start();
		String payload = new Date().toString();

		RedisTemplate<String, String> redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.afterPropertiesSet();

		redisTemplate.boundListOps(TEST_QUEUE)
				.leftPush("{\"payload\":\"" + payload + "\",\"headers\":{}}");

		Message<?> receive = this.fromChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(payload);
		CountDownLatch stopLatch = new CountDownLatch(1);
		this.fromChannelEndpoint.stop(stopLatch::countDown);
		assertThat(stopLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	@RedisAvailable
	public void testInt3017IntegrationSymmetrical() throws InterruptedException {
		this.symmetricalRedisChannelEndpoint.start();
		UUID payload = UUID.randomUUID();
		Message<UUID> message = MessageBuilder.withPayload(payload)
				.setHeader("redis_queue", TEST_QUEUE)
				.build();

		this.symmetricalInputChannel.send(message);

		Message<?> receive = this.symmetricalOutputChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(payload);

		CountDownLatch stopLatch = new CountDownLatch(1);
		this.symmetricalRedisChannelEndpoint.stop(stopLatch::countDown);
		assertThat(stopLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testInt3442ProperlyStop() throws Exception {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		while (redisTemplate.boundListOps(TEST_QUEUE).rightPop() != null) {
			// drain
		}

		RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(TEST_QUEUE,
				this.connectionFactory);
		BoundListOperations<String, byte[]> boundListOperations =
				TestUtils.getPropertyValue(endpoint, "boundListOperations", BoundListOperations.class);
		boundListOperations = Mockito.spy(boundListOperations);
		DirectFieldAccessor dfa = new DirectFieldAccessor(endpoint);
		dfa.setPropertyValue("boundListOperations", boundListOperations);
		endpoint.setBeanFactory(Mockito.mock(BeanFactory.class));
		endpoint.setOutputChannel(new DirectChannel());
		endpoint.setReceiveTimeout(10);

		ExecutorService executorService = Executors.newCachedThreadPool();
		endpoint.setTaskExecutor(executorService);

		endpoint.afterPropertiesSet();
		endpoint.start();

		waitListening(endpoint);
		dfa.setPropertyValue("listening", false);

		redisTemplate.boundListOps(TEST_QUEUE).leftPush("foo");

		CountDownLatch stopLatch = new CountDownLatch(1);

		endpoint.stop(stopLatch::countDown);

		executorService.shutdown();
		assertThat(executorService.awaitTermination(20, TimeUnit.SECONDS)).isTrue();

		assertThat(stopLatch.await(21, TimeUnit.SECONDS)).isTrue();

		verify(boundListOperations, atLeastOnce()).rightPush(any(byte[].class));
	}


	@Test
	@RedisAvailable
	@Ignore("LettuceConnectionFactory doesn't support proper reinitialization after 'destroy()'")
	public void testInt3196Recovery() throws Exception {
		QueueChannel channel = new QueueChannel();

		final List<ApplicationEvent> exceptionEvents = new ArrayList<>();

		final CountDownLatch exceptionsLatch = new CountDownLatch(2);

		RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(TEST_QUEUE,
				this.connectionFactory);
		endpoint.setBeanFactory(Mockito.mock(BeanFactory.class));
		endpoint.setApplicationEventPublisher(event -> {
			exceptionEvents.add((ApplicationEvent) event);
			exceptionsLatch.countDown();
		});
		endpoint.setOutputChannel(channel);
		endpoint.setReceiveTimeout(100);
		endpoint.setRecoveryInterval(200);
		endpoint.afterPropertiesSet();
		endpoint.start();

		waitListening(endpoint);

		((DisposableBean) this.connectionFactory).destroy();

		assertThat(exceptionsLatch.await(10, TimeUnit.SECONDS)).isTrue();

		for (ApplicationEvent exceptionEvent : exceptionEvents) {
			assertThat(exceptionEvent).isInstanceOf(RedisExceptionEvent.class);
			assertThat(exceptionEvent.getSource()).isSameAs(endpoint);
			assertThat(((IntegrationEvent) exceptionEvent).getCause().getClass())
					.isIn(RedisSystemException.class, RedisConnectionFailureException.class);
		}

		((InitializingBean) this.connectionFactory).afterPropertiesSet();

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(this.getConnectionFactoryForTest());
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		String payload = "testing";

		redisTemplate.boundListOps(TEST_QUEUE).leftPush(payload);

		Message<?> receive = channel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(payload);

		CountDownLatch stopLatch = new CountDownLatch(1);
		endpoint.stop(stopLatch::countDown);
		assertThat(stopLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testInt3932ReadFromLeft() throws InterruptedException {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		String payload = "testing";

		redisTemplate.boundListOps(TEST_QUEUE).rightPush(payload);

		Date payload2 = new Date();

		redisTemplate.boundListOps(TEST_QUEUE).rightPush(payload2);

		PollableChannel channel = new QueueChannel();

		RedisQueueMessageDrivenEndpoint endpoint =
				new RedisQueueMessageDrivenEndpoint(TEST_QUEUE, this.connectionFactory);
		endpoint.setBeanFactory(Mockito.mock(BeanFactory.class));
		endpoint.setBeanClassLoader(ClassUtils.getDefaultClassLoader());
		endpoint.setOutputChannel(channel);
		endpoint.setReceiveTimeout(10);
		endpoint.setRightPop(false);
		endpoint.afterPropertiesSet();
		endpoint.start();

		Message<Object> receive = (Message<Object>) channel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(payload);

		receive = (Message<Object>) channel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(payload2);

		CountDownLatch stopLatch = new CountDownLatch(1);
		endpoint.stop(stopLatch::countDown);
		assertThat(stopLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	private void waitListening(RedisQueueMessageDrivenEndpoint endpoint) throws InterruptedException {
		int n = 0;
		do {
			n++;
			if (n == 100) {
				break;
			}
			Thread.sleep(100);
		}
		while (!endpoint.isListening());

		assertThat(n < 100).isTrue();
	}

}
