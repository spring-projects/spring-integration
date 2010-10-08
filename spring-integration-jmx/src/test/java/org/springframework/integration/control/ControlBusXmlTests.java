/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.control;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.monitor.LifecycleMessageHandlerMetrics;
import org.springframework.integration.monitor.QueueChannelMetrics;
import org.springframework.integration.monitor.DirectChannelMetrics;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ControlBusXmlTests {

	private static final String DOMAIN = "control.bus.xml.test";


	@Autowired
	private MBeanServer mbeanServer;


	@Test
	public void directChannelRegistered() throws Exception {
		ObjectInstance instance = mbeanServer.getObjectInstance(
				ObjectNameManager.getInstance(DOMAIN + ":type=MessageChannel,name=testDirectChannel"));
		assertEquals(DirectChannelMetrics.class.getName(), instance.getClassName());
	}

	@Test
	public void queueChannelRegistered() throws Exception {
		ObjectInstance instance = mbeanServer.getObjectInstance(
				ObjectNameManager.getInstance(DOMAIN + ":type=MessageChannel,name=testQueueChannel"));
		assertEquals(QueueChannelMetrics.class.getName(), instance.getClassName());
	}

	@Test
	public void eventDrivenConsumerRegistered() throws Exception {
		ObjectInstance instance = mbeanServer.getObjectInstance(
				ObjectNameManager.getInstance(DOMAIN + ":type=MessageHandler,name=testEventDrivenBridge,bean=endpoint"));
		assertEquals(LifecycleMessageHandlerMetrics.class.getName(), instance.getClassName());
	}

	@Test
	public void anonymousConsumerRegistered() throws Exception {
		Set<ObjectInstance> instances = mbeanServer.queryMBeans(
				ObjectNameManager.getInstance(DOMAIN + ":type=MessageHandler,bean=anonymous,*"), null);
		assertEquals(1, instances.size());
		assertEquals(LifecycleMessageHandlerMetrics.class.getName(), instances.iterator().next().getClassName());
	}

	@Test
	public void pollingConsumerRegistered() throws Exception {
		ObjectInstance instance = mbeanServer.getObjectInstance(
				ObjectNameManager.getInstance(DOMAIN + ":type=MessageHandler,name=testPollingBridge,bean=endpoint"));
		assertEquals(LifecycleMessageHandlerMetrics.class.getName(), instance.getClassName());
	}

}
