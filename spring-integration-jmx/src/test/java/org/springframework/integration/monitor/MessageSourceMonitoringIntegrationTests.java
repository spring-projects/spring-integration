/*
 * Copyright 2009-2010 the original author or authors.
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

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.PollableChannel;

public class MessageSourceMonitoringIntegrationTests {

	private PollableChannel channel;

	private Service service;

	private IntegrationMBeanExporter exporter;
	
	public void setMessageHandlersMonitor(IntegrationMBeanExporter exporter) {
		this.exporter = exporter;
	}
	
	public void setService(Service service) {
		this.service = service;
	}

	@Test
	public void testSendAndHandleWithEndpointName() throws Exception {
		// The message source monitor is registered under the endpoint id (since it is explicit)
		doTest("explicit-source.xml", "input", "explicit");
	}

	@Test
	public void testSendAndHandleWithAnonymous() throws Exception {
		// The message source monitor is registered under the channel name
		doTest("anonymous-source.xml", "anonymous", "anonymous");
	}

	private void doTest(String config, String channelName, String monitor) throws Exception {

		ClassPathXmlApplicationContext context = createContext(config, channelName);

		try {

			int before = service.getCounter();
			channel.receive(1000L);
			channel.receive(1000L);
			assertTrue(before < service.getCounter());

			int count = exporter.getSourceMessageCount(monitor);
			assertTrue("No statistics for input channel", count > 0);

		} finally {
			context.close();
		}

	}

	private ClassPathXmlApplicationContext createContext(String config, String channelName) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config, getClass());
		context.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		channel = context.getBean(channelName, PollableChannel.class);
		return context;
	}

	public static interface Service {
		String execute() throws Exception;
		int getCounter();
	}
	
	public static class SimpleService implements Service {
		private int counter;

		public String execute() throws Exception {
			Thread.sleep(10L); // make the duration non-zero
			counter++;
			return "count="+counter;
		}

		public int getCounter() {
			return counter;
		}
	}
	
}
