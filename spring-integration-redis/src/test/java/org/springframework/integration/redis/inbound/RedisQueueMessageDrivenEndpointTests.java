/*
 * Copyright 2013 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.redis.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
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
import org.springframework.context.ApplicationEventPublisher;
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
import org.springframework.integration.event.IntegrationEvent;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisQueueMessageDrivenEndpointTests extends RedisAvailableTests {

	@Autowired
	private RedisConnectionFactory connectionFactory;

	@Autowired
	private PollableChannel fromChannel;

	@Autowired
	private MessageChannel symmetricalInputChannel;

	@Autowired
	private PollableChannel symmetricalOutputChannel;

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testInt3014Default() throws Exception {

		String queueName = "si.test.redisQueueInboundChannelAdapterTests";

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		String payload = "testing";

		redisTemplate.boundListOps(queueName).leftPush(payload);

		Date payload2 = new Date();

		redisTemplate.boundListOps(queueName).leftPush(payload2);

		PollableChannel channel = new QueueChannel();

		RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(queueName, this.connectionFactory);
		endpoint.setBeanFactory(Mockito.mock(BeanFactory.class));
		endpoint.setOutputChannel(channel);
		endpoint.setReceiveTimeout(1000);
		endpoint.afterPropertiesSet();
		endpoint.start();

		Message<Object> receive = (Message<Object>) channel.receive(2000);
		assertNotNull(receive);
		assertEquals(payload, receive.getPayload());

		receive = (Message<Object>) channel.receive(2000);
		assertNotNull(receive);
		assertEquals(payload2, receive.getPayload());

		endpoint.stop();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testInt3014ExpectMessageTrue() throws Exception {

		final String queueName = "si.test.redisQueueInboundChannelAdapterTests2";

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		Message<?> message = MessageBuilder.withPayload("testing").build();

		redisTemplate.boundListOps(queueName).leftPush(message);

		redisTemplate.boundListOps(queueName).leftPush("test");

		PollableChannel channel = new QueueChannel();

		PollableChannel errorChannel = new QueueChannel();

		RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(queueName, this.connectionFactory);
		endpoint.setBeanFactory(Mockito.mock(BeanFactory.class));
		endpoint.setExpectMessage(true);
		endpoint.setOutputChannel(channel);
		endpoint.setErrorChannel(errorChannel);
		endpoint.setReceiveTimeout(1000);
		endpoint.afterPropertiesSet();
		endpoint.start();

		Message<Object> receive = (Message<Object>) channel.receive(2000);
		assertNotNull(receive);

		assertEquals(message, receive);

		receive = (Message<Object>) errorChannel.receive(2000);
		assertNotNull(receive);
		assertThat(receive, Matchers.instanceOf(ErrorMessage.class));
		assertThat(receive.getPayload(), Matchers.instanceOf(MessagingException.class));
		assertThat(((Exception) receive.getPayload()).getMessage(), Matchers.containsString("Deserialization of Message failed."));
		assertThat(((Exception) receive.getPayload()).getCause(), Matchers.instanceOf(ClassCastException.class));
		assertThat(((Exception) receive.getPayload()).getCause().getMessage(),
				Matchers.containsString("java.lang.String cannot be cast to org.springframework.messaging.Message"));

		endpoint.stop();
	}

	@Test
	@RedisAvailable
	public void testInt3017IntegrationInbound() throws Exception {

		String payload = new Date().toString();

		RedisTemplate<String, String> redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.afterPropertiesSet();

		redisTemplate.boundListOps("si.test.Int3017IntegrationInbound").leftPush("{\"payload\":\"" + payload + "\",\"headers\":{}}");

		Message<?> receive = this.fromChannel.receive(2000);
		assertNotNull(receive);
		assertEquals(payload, receive.getPayload());
	}

	@Test
	@RedisAvailable
	public void testInt3017IntegrationSymmetrical() throws Exception {
		UUID payload = UUID.randomUUID();
		Message<UUID> message = MessageBuilder.withPayload(payload)
				.setHeader("redis_queue", "si.test.Int3017IntegrationSymmetrical")
				.build();

		this.symmetricalInputChannel.send(message);

		Message<?> receive = this.symmetricalOutputChannel.receive(2000);
		assertNotNull(receive);
		assertEquals(payload, receive.getPayload());
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testInt3442ProperlyStop() throws Exception {
		final String queueName = "si.test.testInt3442ProperlyStopTest";

		final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(queueName,
				this.connectionFactory);
		BoundListOperations<String, byte[]> boundListOperations =
				TestUtils.getPropertyValue(endpoint, "boundListOperations", BoundListOperations.class);
		boundListOperations = Mockito.spy(boundListOperations);
		new DirectFieldAccessor(endpoint).setPropertyValue("boundListOperations", boundListOperations);
		endpoint.setBeanFactory(Mockito.mock(BeanFactory.class));
		endpoint.setOutputChannel(new DirectChannel());
		endpoint.setReceiveTimeout(1000);
		endpoint.setStopTimeout(100);

		ExecutorService executorService = Executors.newCachedThreadPool();
		endpoint.setTaskExecutor(executorService);

		endpoint.afterPropertiesSet();
		endpoint.start();

		redisTemplate.boundListOps(queueName).leftPush("foo");
		endpoint.stop();

		executorService.shutdown();
		assertTrue(executorService.awaitTermination(1, TimeUnit.SECONDS));

		Mockito.verify(boundListOperations).rightPush(Mockito.any(byte[].class));
	}


	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	@Ignore
	//JedisConnectionFactory doesn't support proper 'destroy()' and allows to create new fresh Redis connection
	public void testInt3196Recovery() throws Exception {
		String queueName = "test.si.Int3196Recovery";
		QueueChannel channel = new QueueChannel();

		final List<ApplicationEvent> exceptionEvents = new ArrayList<ApplicationEvent>();

		final CountDownLatch exceptionsLatch = new CountDownLatch(2);

		RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(queueName, this.connectionFactory);
		endpoint.setBeanFactory(Mockito.mock(BeanFactory.class));
		endpoint.setApplicationEventPublisher(new ApplicationEventPublisher() {

			@Override
			public void publishEvent(ApplicationEvent event) {
				exceptionEvents.add(event);
				exceptionsLatch.countDown();
			}
		});
		endpoint.setOutputChannel(channel);
		endpoint.setReceiveTimeout(100);
		endpoint.setRecoveryInterval(200);
		endpoint.afterPropertiesSet();
		endpoint.start();

		int n = 0;
		do {
			n++;
			if (n == 100) {
				break;
			}
			Thread.sleep(100);
		} while (!endpoint.isListening());

		assertTrue(n < 100);

		((DisposableBean) this.connectionFactory).destroy();

		assertTrue(exceptionsLatch.await(10, TimeUnit.SECONDS));

		for (ApplicationEvent exceptionEvent : exceptionEvents) {
			assertThat(exceptionEvent, Matchers.instanceOf(RedisExceptionEvent.class));
			assertSame(endpoint, exceptionEvent.getSource());
			assertThat(((IntegrationEvent) exceptionEvent).getCause().getClass(),
					Matchers.isIn(Arrays.<Class<? extends Throwable>>asList(RedisSystemException.class, RedisConnectionFailureException.class)));
		}

		((InitializingBean) this.connectionFactory).afterPropertiesSet();

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(this.getConnectionFactoryForTest());
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		String payload = "testing";

		redisTemplate.boundListOps(queueName).leftPush(payload);

		Message<?> receive = channel.receive(1000);
		assertNotNull(receive);
		assertEquals(payload, receive.getPayload());

		endpoint.stop();
	}

}
