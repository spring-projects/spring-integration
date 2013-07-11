/*
 * Copyright 2007-2013 the original author or authors
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.json.JacksonJsonObjectMapperProvider;
import org.springframework.integration.json.JsonObjectMapper;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gunnar Hillert
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class RedisQueueInboundChannelAdapterTests extends RedisAvailableTests{

	private final Log logger = LogFactory.getLog(this.getClass());

	@Autowired
	private GenericApplicationContext context;

	@Autowired
	@Qualifier("redisPoller")
	private PollerMetadata poller;

	@Autowired
	private PollableChannel errorChannel;

	@Autowired
	private JedisConnectionFactory connectionFactory;

	@Test
	@RedisAvailable
	public void testRedisQueueInboundChannelAdapter() throws Exception {

		final String queueName = "si.test.redisQueueInboundChannelAdapterTests";

		final DirectChannel inboundChannel = new DirectChannel();

		final RedisQueueMessageSource messageSource = new RedisQueueMessageSource(queueName, connectionFactory);
		messageSource.setMessageReturned(false);
		messageSource.setRedisTimeout(4000);
		final SourcePollingChannelAdapter adapter = getSourcePollingChannelAdapter(
				messageSource, inboundChannel, poller, context, this.getClass().getClassLoader());
		adapter.start();

		final CountDownLatch messageReceived = new CountDownLatch(1);
		inboundChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				logger.debug("Received Message: " + message);
				assertEquals("hello", message.getPayload());
				messageReceived.countDown();
			}
		});

		final StringRedisTemplate redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();

		redisTemplate.boundListOps(queueName).leftPush("hello");

		assertTrue("The messageReceived CountDownLatch was not zero.", messageReceived.await(5000, TimeUnit.MILLISECONDS));

	}

	@Test
	@RedisAvailable
	public void testRedisQueueInboundChannelAdapter2() throws Exception {

		final String queueName = "si.test.redisQueueInboundChannelAdapterTests2";

		final DirectChannel inboundChannel = new DirectChannel();

		final RedisQueueMessageSource messageSource = new RedisQueueMessageSource(queueName, connectionFactory);
		messageSource.setMessageReturned(true);
		messageSource.setRedisTimeout(4000);
		final SourcePollingChannelAdapter adapter = getSourcePollingChannelAdapter(
				messageSource, inboundChannel, poller, context, this.getClass().getClassLoader());
		adapter.start();

		final Message<String> message = MessageBuilder.withPayload("foobar").setHeader("myHeader", "123").build();

		final CountDownLatch messageReceived = new CountDownLatch(1);
		inboundChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> localMessage) throws MessagingException {
				logger.debug("Received Message: " + localMessage);
				assertEquals(message.getPayload(), localMessage.getPayload());
				assertEquals(message.getHeaders().size(), localMessage.getHeaders().size());
				assertEquals(message.getHeaders().get("myHeader", String.class), localMessage.getHeaders().get("myHeader", String.class));
				messageReceived.countDown();
			}
		});

		final StringRedisTemplate redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();

		final JsonObjectMapper<?> objectMapper = JacksonJsonObjectMapperProvider.newInstance();

		redisTemplate.boundListOps(queueName).leftPush(objectMapper.toJson(message));

		assertTrue("The messageReceived CountDownLatch was not zero.", messageReceived.await(5000, TimeUnit.MILLISECONDS));

	}

	@Test
	@RedisAvailable
	public void testRedisQueueInboundChannelAdapterWithDeserializationError() throws Exception {

		final String queueName = "si.test.testRedisQueueInboundChannelAdapterWithDeserializationError";

		final DirectChannel inboundChannel = new DirectChannel();

		final RedisQueueMessageSource messageSource = new RedisQueueMessageSource(queueName, connectionFactory);
		messageSource.setMessageReturned(true);
		final SourcePollingChannelAdapter adapter = getSourcePollingChannelAdapter(
				messageSource, inboundChannel, poller, context, this.getClass().getClassLoader());
		adapter.start();

		inboundChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> localMessage) throws MessagingException {
				logger.debug("Received Message: " + localMessage);
			}
		});

		final StringRedisTemplate redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();

		redisTemplate.boundListOps(queueName).leftPush("thisShouldFail");

		Message<?> message = errorChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message instanceof ErrorMessage);
		assertEquals("Deserialization of Message failed.", ((ErrorMessage) message).getPayload().getMessage());

	}

	public static SourcePollingChannelAdapter getSourcePollingChannelAdapter(MessageSource<?> adapter,
			MessageChannel channel,
			PollerMetadata poller,
			GenericApplicationContext applicationContext,
			ClassLoader beanClassLoader) throws Exception {

		SourcePollingChannelAdapterFactoryBean fb = new SourcePollingChannelAdapterFactoryBean();
		fb.setSource(adapter);
		fb.setOutputChannel(channel);
		fb.setPollerMetadata(poller);
		fb.setBeanClassLoader(beanClassLoader);
		fb.setAutoStartup(false);
		fb.setBeanFactory(applicationContext.getBeanFactory());
		fb.afterPropertiesSet();

		return fb.getObject();
	}

}
