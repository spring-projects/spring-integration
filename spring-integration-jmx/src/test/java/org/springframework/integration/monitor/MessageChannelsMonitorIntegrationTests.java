/*
 * Copyright 2009-2019 the original author or authors.
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

package org.springframework.integration.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.InterceptableChannel;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class MessageChannelsMonitorIntegrationTests {

	private static Log logger = LogFactory.getLog(MessageChannelsMonitorIntegrationTests.class);

	private MessageChannel channel;

	private Service service;

	private IntegrationMBeanExporter messageChannelsMonitor;

	public void setMessageHandlersMonitor(IntegrationMBeanExporter messageChannelsMonitor) {
		this.messageChannelsMonitor = messageChannelsMonitor;
	}

	public void setService(Service service) {
		this.service = service;
	}

	@Test
	public void testSendWithAnonymousHandler() throws Exception {
		doTest("anonymous-channel.xml", "anonymous");
	}

	@Test
	public void testSendWithProxiedChannel() throws Exception {
		doTest("proxy-channel.xml", "anonymous");
	}

	@Test
	public void testRates() throws Exception {
		try (ClassPathXmlApplicationContext context = createContext("anonymous-channel.xml")) {
			this.channel = context.getBean("anonymous", MessageChannel.class);
			int before = service.getCounter();
			CountDownLatch latch = new CountDownLatch(50);
			service.setLatch(latch);
			for (int i = 0; i < 50; i++) {
				channel.send(new GenericMessage<>("bar"));
				Thread.sleep(20L);
			}
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(service.getCounter()).isEqualTo(before + 50);

			// The handler monitor is registered under the endpoint id (since it is explicit)
			int sends = messageChannelsMonitor.getChannelSendRate("" + channel).getCount();
			assertThat(sends).as("No send statistics for input channel").isEqualTo(50);
			long sendsLong = messageChannelsMonitor.getChannelSendRate("" + channel).getCountLong();
			assertThat(sends).as("No send statistics for input channel").isEqualTo(sendsLong);

		}
	}

	@Test
	public void testErrors() throws Exception {
		try (ClassPathXmlApplicationContext context = createContext("anonymous-channel.xml")) {
			this.channel = context.getBean("anonymous", MessageChannel.class);
			int before = service.getCounter();
			CountDownLatch latch = new CountDownLatch(10);
			service.setLatch(latch);
			for (int i = 0; i < 5; i++) {
				channel.send(new GenericMessage<>("bar"));
				Thread.sleep(20L);
			}
			try {
				channel.send(new GenericMessage<>("fail"));
			}
			catch (MessageHandlingException e) {
				// ignore
			}
			for (int i = 0; i < 5; i++) {
				channel.send(new GenericMessage<>("bar"));
				Thread.sleep(20L);
			}
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(service.getCounter()).isEqualTo(before + 10);

			// The handler monitor is registered under the endpoint id (since it is explicit)
			int sends = messageChannelsMonitor.getChannelSendRate("" + channel).getCount();
			assertThat(sends).as("No send statistics for input channel").isEqualTo(11);
			int errors = messageChannelsMonitor.getChannelErrorRate("" + channel).getCount();
			assertThat(errors).as("No error statistics for input channel").isEqualTo(1);
		}
	}

	@Test
	public void testQueues() throws Exception {
		try (ClassPathXmlApplicationContext context = createContext("queue-channel.xml")) {
			this.channel = context.getBean("queue", MessageChannel.class);
			int before = service.getCounter();
			CountDownLatch latch = new CountDownLatch(10);
			service.setLatch(latch);
			for (int i = 0; i < 5; i++) {
				channel.send(new GenericMessage<>("bar"));
				Thread.sleep(20L);
			}
			try {
				channel.send(new GenericMessage<>("fail"));
			}
			catch (MessageHandlingException e) {
				// ignore
			}
			for (int i = 0; i < 5; i++) {
				channel.send(new GenericMessage<>("bar"));
				Thread.sleep(20L);
			}
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(service.getCounter()).isEqualTo(before + 10);

			// The handler monitor is registered under the endpoint id (since it is explicit)
			int sends = messageChannelsMonitor.getChannelSendRate("" + channel).getCount();
			assertThat(sends).as("No send statistics for input channel").isEqualTo(11);
			int receives = messageChannelsMonitor.getChannelReceiveCount("" + channel);
			assertThat(receives).as("No send statistics for input channel").isEqualTo(11);
			int errors = messageChannelsMonitor.getChannelErrorRate("" + channel).getCount();
			assertThat(errors).as("Expect no errors for input channel (handler fails)").isEqualTo(0);
		}
	}

	private void doTest(String config, String channelName) throws Exception {
		try (ClassPathXmlApplicationContext context = createContext(config)) {
			this.channel = context.getBean(channelName, MessageChannel.class);
			int before = service.getCounter();
			CountDownLatch latch = new CountDownLatch(1);
			service.setLatch(latch);
			channel.send(new GenericMessage<>("bar"));
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(service.getCounter()).isEqualTo(before + 1);

			// The handler monitor is registered under the endpoint id (since it is explicit)
			int sends = messageChannelsMonitor.getChannelSendRate("" + channel).getCount();
			assertThat(sends).as("No statistics for input channel").isEqualTo(1);

			assertThat(channel).isInstanceOf(InterceptableChannel.class);
			List<ChannelInterceptor> channelInterceptors = ((InterceptableChannel) channel).getInterceptors();
			assertThat(channelInterceptors.size()).isEqualTo(1);
			assertThat(channelInterceptors.get(0)).isInstanceOf(WireTap.class);
		}
	}

	private ClassPathXmlApplicationContext createContext(String config) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config, getClass());
		context.getAutowireCapableBeanFactory()
				.autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		return context;
	}

	public static class Service {

		private int counter;

		private volatile CountDownLatch latch;

		public void setLatch(CountDownLatch latch) {
			this.latch = latch;
		}

		public void execute(String input) {
			if ("fail".equals(input)) {
				throw new RuntimeException("Planned");
			}
			counter++;
			latch.countDown();
		}

		public int getCounter() {
			return counter;
		}

	}

	@Aspect
	public static class TestChannelInterceptor {

		@Before("execution(* *..MessageChannel+.send(*)) && args(input)")
		public void around(Message<?> input) {
			logger.debug("Handling: " + input);
		}

	}

}
