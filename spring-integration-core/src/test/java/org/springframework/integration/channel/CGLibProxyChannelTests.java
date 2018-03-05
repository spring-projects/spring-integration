/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @since 4.3.10
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class CGLibProxyChannelTests {

	@Autowired
	private DirectChannel directChannel;

	@Autowired
	private QueueChannel queueChannel;

	@Autowired
	private ExecutorChannel executorChannel;

	@Autowired
	private PublishSubscribeChannel publishSubscribeChannel;

	@Test
	public void testProxyDirect() {
		assertTrue(AopUtils.isCglibProxy(this.directChannel));
		final AtomicReference<Message<?>> message = new AtomicReference<>();
		this.directChannel.subscribe(m -> message.set(m));
		this.directChannel.send(new GenericMessage<>("foo"));
		assertNotNull(message.get());
	}

	@Test
	public void testProxyQueue() {
		assertTrue(AopUtils.isCglibProxy(this.queueChannel));
		this.queueChannel.send(new GenericMessage<>("foo"));
		assertNotNull(this.queueChannel.receive(0));
	}

	@Test
	public void testProxyExecutor() throws Exception {
		assertTrue(AopUtils.isCglibProxy(this.executorChannel));
		final AtomicReference<Message<?>> message = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		this.executorChannel.subscribe(m -> {
			message.set(m);
			latch.countDown();
		});
		this.executorChannel.send(new GenericMessage<>("foo"));
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertNotNull(message.get());
	}

	@Test
	public void testProxyPubSubWithExec() throws Exception {
		assertTrue(AopUtils.isCglibProxy(this.publishSubscribeChannel));
		final AtomicReference<Message<?>> message = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		this.publishSubscribeChannel.subscribe(m -> {
			message.set(m);
			latch.countDown();
		});
		this.publishSubscribeChannel.send(new GenericMessage<>("foo"));
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertNotNull(message.get());
	}

	@Configuration
	public static class Config {

		@Bean
		public ProxyFactoryBean directChannel() {
			return createProxyFactory(new DirectChannel());
		}

		@Bean
		public ProxyFactoryBean queueChannel() {
			return createProxyFactory(new QueueChannel());
		}

		@Bean
		public ProxyFactoryBean executorChannel() {
			return createProxyFactory(new ExecutorChannel(executor()));
		}

		@Bean
		public ProxyFactoryBean publishSubscribeChannel() {
			return createProxyFactory(new PublishSubscribeChannel(executor()));
		}

		@Bean
		public Executor executor() {
			return new ThreadPoolTaskExecutor();
		}

		private ProxyFactoryBean createProxyFactory(MessageChannel channel) {
			ProxyFactoryBean fb = new ProxyFactoryBean();
			fb.setProxyTargetClass(true);
			fb.setTarget(channel);
			return fb;
		}

	}

}
