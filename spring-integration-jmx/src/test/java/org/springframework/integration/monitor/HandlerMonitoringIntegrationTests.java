/*
 * Copyright 2009-2020 the original author or authors.
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

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 */
public class HandlerMonitoringIntegrationTests {

	private static final Log logger = LogFactory.getLog(HandlerMonitoringIntegrationTests.class);

	private MessageChannel channel;

	private Service service;

	private IntegrationMBeanExporter messageHandlersMonitor;

	public void setMessageHandlersMonitor(IntegrationMBeanExporter messageHandlersMonitor) {
		this.messageHandlersMonitor = messageHandlersMonitor;
	}

	public void setService(Service service) {
		this.service = service;
	}

	@Test
	public void testSendAndHandleWithEndpointName() {
		// The handler monitor is registered under the endpoint id (since it is explicit)
		doTest("explicit-handler.xml", "input", "explicit");
	}

	@Test
	public void testSendAndHandleWithAnonymousHandler() {
		doTest("anonymous-handler.xml", "anonymous", "anonymous");
	}

	@Test
	public void testSendAndHandleWithProxiedHandler() {
		doTest("proxy-handler.xml", "anonymous", "anonymous");
	}

	@Test
	public void testErrorLogger() {
		ClassPathXmlApplicationContext context = createContext("anonymous-handler.xml", "anonymous");
		try {
			assertThat(Arrays.asList(messageHandlersMonitor.getHandlerNames()).contains("errorLogger")).isTrue();
		}
		finally {
			context.close();
		}

	}

	private void doTest(String config, String channelName, String monitor) {
		ClassPathXmlApplicationContext context = createContext(config, channelName);
		try {
			int before = service.getCounter();
			channel.send(new GenericMessage<>("bar"));
			assertThat(service.getCounter()).isEqualTo(before + 1);
		}
		finally {
			context.close();
		}
	}

	private ClassPathXmlApplicationContext createContext(String config, String channelName) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config, getClass());
		context.getAutowireCapableBeanFactory()
				.autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		channel = context.getBean(channelName, MessageChannel.class);
		return context;
	}

	public interface Service {

		void execute(String input) throws Exception;

		int getCounter();

	}

	public static class SimpleService implements Service {

		private int counter;

		@Override
		public void execute(String input) throws Exception {
			Thread.sleep(10L); // make the duration non-zero
			this.counter++;
		}

		@Override
		public int getCounter() {
			return counter;
		}

	}

	@Aspect
	public static class HandlerInterceptor {

		@Before("execution(* *..*Tests*(String)) && args(input)")
		public void around(String input) {
			logger.debug("Handling: " + input);
		}

	}

}
