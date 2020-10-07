/*
 * Copyright 2014-2019 the original author or authors.
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

package org.springframework.integration.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class DownstreamExceptionTests {

	@ClassRule
	public static final BrokerRunning brokerRunning = BrokerRunning.isRunning(1883);

	@Autowired
	private Service service;

	@Autowired
	private MqttPahoMessageDrivenChannelAdapter noErrorChannel;

	@Autowired
	private MqttPahoMessageDrivenChannelAdapter withErrorChannel;

	@Autowired
	private PollableChannel errors;

	@Test
	@SuppressWarnings("unchecked")
	public void testNoErrorChannel() throws Exception {
		service.n = 0;
		LogAccessor logger = spy(TestUtils.getPropertyValue(noErrorChannel, "logger", LogAccessor.class));
		final CountDownLatch latch = new CountDownLatch(1);
		doAnswer(invocation -> {
			if (((Supplier<String>) invocation.getArgument(1)).get().contains("Unhandled")) {
				latch.countDown();
			}
			return null;
		}).when(logger).error(any(Throwable.class), any(Supplier.class));
		new DirectFieldAccessor(noErrorChannel).setPropertyValue("logger", logger);
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-fooEx1");
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		adapter.handleMessage(new GenericMessage<>("foo"));
		service.barrier.await(10, TimeUnit.SECONDS);
		service.barrier.reset();
		adapter.handleMessage(new GenericMessage<>("foo"));
		service.barrier.await(10, TimeUnit.SECONDS);
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		verify(logger).error(any(Throwable.class),
				ArgumentMatchers.<Supplier<String>>argThat(logMessage ->
						logMessage.get().startsWith("Unhandled exception for")));
		service.barrier.reset();
		adapter.stop();
	}

	@Test
	public void testWithErrorChannel() throws Exception {
		assertThat(TestUtils.getPropertyValue(this.withErrorChannel, "errorChannel")).isSameAs(this.errors);
		service.n = 0;
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-fooEx2");
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		adapter.handleMessage(new GenericMessage<>("foo"));
		service.barrier.await(10, TimeUnit.SECONDS);
		service.barrier.reset();
		adapter.handleMessage(new GenericMessage<>("foo"));
		service.barrier.await(10, TimeUnit.SECONDS);
		assertThat(errors.receive(10000)).isNotNull();
		service.barrier.reset();
		adapter.stop();
	}

	public static class Service {

		public CyclicBarrier barrier = new CyclicBarrier(2);

		public int n;

		public void foo(String foo) throws Exception {
			barrier.await(10, TimeUnit.SECONDS);
			if (n++ > 0) {
				throw new RuntimeException("bar");
			}
		}

	}

}
