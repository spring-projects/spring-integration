/*
 * Copyright 2009-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;

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

		ClassPathXmlApplicationContext context = createContext("anonymous-channel.xml", "anonymous");

		try {

			int before = service.getCounter();
			CountDownLatch latch = new CountDownLatch(50);
			service.setLatch(latch);
			for (int i = 0; i < 50; i++) {
				channel.send(new GenericMessage<String>("bar"));
				Thread.sleep(20L);
			}
			assertTrue(latch.await(10, TimeUnit.SECONDS));
			assertEquals(before + 50, service.getCounter());

			// The handler monitor is registered under the endpoint id (since it is explicit)
			int sends = messageChannelsMonitor.getChannelSendRate("" + channel).getCount();
			assertEquals("No send statistics for input channel", 50, sends, 0.01);

		}
		finally {
			context.close();
		}

	}

	@Test
	public void testErrors() throws Exception {

		ClassPathXmlApplicationContext context = createContext("anonymous-channel.xml", "anonymous");
		try {
			int before = service.getCounter();
			CountDownLatch latch = new CountDownLatch(10);
			service.setLatch(latch);
			for (int i = 0; i < 5; i++) {
				channel.send(new GenericMessage<String>("bar"));
				Thread.sleep(20L);
			}
			try {
				channel.send(new GenericMessage<String>("fail"));
			}
			catch (MessageHandlingException e) {
				// ignore
			}
			for (int i = 0; i < 5; i++) {
				channel.send(new GenericMessage<String>("bar"));
				Thread.sleep(20L);
			}
			assertTrue(latch.await(10, TimeUnit.SECONDS));
			assertEquals(before + 10, service.getCounter());

			// The handler monitor is registered under the endpoint id (since it is explicit)
			int sends = messageChannelsMonitor.getChannelSendRate("" + channel).getCount();
			assertEquals("No send statistics for input channel", 11, sends, 0.01);
			int errors = messageChannelsMonitor.getChannelErrorRate("" + channel).getCount();
			assertEquals("No error statistics for input channel", 1, errors, 0.01);
		}
		finally {
			context.close();
		}

	}

	@Test
	public void testQueues() throws Exception {

		ClassPathXmlApplicationContext context = createContext("queue-channel.xml", "queue");
		try {
			int before = service.getCounter();
			CountDownLatch latch = new CountDownLatch(10);
			service.setLatch(latch);
			for (int i = 0; i < 5; i++) {
				channel.send(new GenericMessage<String>("bar"));
				Thread.sleep(20L);
			}
			try {
				channel.send(new GenericMessage<String>("fail"));
			}
			catch (MessageHandlingException e) {
				// ignore
			}
			for (int i = 0; i < 5; i++) {
				channel.send(new GenericMessage<String>("bar"));
				Thread.sleep(20L);
			}
			assertTrue(latch.await(10, TimeUnit.SECONDS));
			assertEquals(before + 10, service.getCounter());

			// The handler monitor is registered under the endpoint id (since it is explicit)
			int sends = messageChannelsMonitor.getChannelSendRate("" + channel).getCount();
			assertEquals("No send statistics for input channel", 11, sends);
			int receives = messageChannelsMonitor.getChannelReceiveCount("" + channel);
			assertEquals("No send statistics for input channel", 11, receives);
			int errors = messageChannelsMonitor.getChannelErrorRate("" + channel).getCount();
			assertEquals("Expect no errors for input channel (handler fails)", 0, errors);
		}
		finally {
			context.close();
		}

	}

	private void doTest(String config, String channelName) throws Exception {

		ClassPathXmlApplicationContext context = createContext(config, channelName);

		try {

			int before = service.getCounter();
			CountDownLatch latch = new CountDownLatch(1);
			service.setLatch(latch);
			channel.send(new GenericMessage<String>("bar"));
			assertTrue(latch.await(10, TimeUnit.SECONDS));
			assertEquals(before + 1, service.getCounter());

			// The handler monitor is registered under the endpoint id (since it is explicit)
			int sends = messageChannelsMonitor.getChannelSendRate("" + channel).getCount();
			assertEquals("No statistics for input channel", 1, sends, 0.01);

			assertThat(channel, Matchers.instanceOf(ChannelInterceptorAware.class));
			List<ChannelInterceptor> channelInterceptors = ((ChannelInterceptorAware) channel).getChannelInterceptors();
			assertEquals(1, channelInterceptors.size());
			assertThat(channelInterceptors.get(0), Matchers.instanceOf(WireTap.class));
		}
		finally {
			context.close();
		}

	}

	private ClassPathXmlApplicationContext createContext(String config, String channelName) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config, getClass());
		context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		channel = context.getBean(channelName, MessageChannel.class);
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
