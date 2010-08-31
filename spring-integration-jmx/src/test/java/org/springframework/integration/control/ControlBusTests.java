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

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.GenericMessage;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class ControlBusTests {

	private volatile GenericApplicationContext context;

	@Before
	public void createContext() {
		this.context = new GenericApplicationContext();
		RootBeanDefinition serverDef = new RootBeanDefinition(MBeanServerFactoryBean.class);
		serverDef.getPropertyValues().add("locateExistingServerIfPossible", true);
		context.registerBeanDefinition("mbeanServer", serverDef);
	}

	@After
	public void closeContext() {
		MBeanServer mbeanServer = this.context.getBean("mbeanServer", MBeanServer.class);
		try {
			MBeanServerFactory.releaseMBeanServer(mbeanServer);
		}
		catch (Exception e) {
			// ignore
		}
		this.context.close();
	}


	@Test
	public void directChannelRegistered() throws Exception {
		context.registerBeanDefinition("directChannel", new RootBeanDefinition(DirectChannel.class));
		BeanDefinition controlBusDef = new RootBeanDefinition(ControlBus.class);
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("mbeanServer"));
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue("domain.test1");
		context.registerBeanDefinition("controlBus", controlBusDef);
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(
				ObjectNameManager.getInstance("domain.test1:type=channel,name=directChannel"));
		assertEquals(DirectChannel.class.getName(), instance.getClassName());
	}

	@Test
	public void anonymousDirectChannelRegistered() throws Exception {
		context.registerBeanDefinition("org.springframework.integration.generated#0", new RootBeanDefinition(DirectChannel.class));
		BeanDefinition controlBusDef = new RootBeanDefinition(ControlBus.class);
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("mbeanServer"));
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue("domain.test1b");
		context.registerBeanDefinition("controlBus", controlBusDef);
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(
				ObjectNameManager.getInstance("domain.test1b:type=channel,name=anonymous,generated=org.springframework.integration.generated#0"));
		assertEquals(DirectChannel.class.getName(), instance.getClassName());
	}

	@Test
	public void staticObjectNamePropertiesRegistered() throws Exception {
		context.registerBeanDefinition("directChannel", new RootBeanDefinition(DirectChannel.class));
		BeanDefinition controlBusDef = new RootBeanDefinition(ControlBus.class);
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("mbeanServer"));
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue("domain.test1a");
		controlBusDef.getPropertyValues().add("objectNameStaticProperties", Collections.singletonMap("foo","bar"));
		context.registerBeanDefinition("controlBus", controlBusDef);
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(
				ObjectNameManager.getInstance("domain.test1a:type=channel,foo=bar,name=directChannel"));
		assertEquals(DirectChannel.class.getName(), instance.getClassName());
	}

	@Test
	public void queueChannelRegistered() throws Exception {
		context.registerBeanDefinition("queueChannel", new RootBeanDefinition(QueueChannel.class));
		BeanDefinition controlBusDef = new RootBeanDefinition(ControlBus.class);
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("mbeanServer"));
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue("domain.test2");
		context.registerBeanDefinition("controlBus", controlBusDef);
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(
				ObjectNameManager.getInstance("domain.test2:type=channel,name=queueChannel"));
		assertEquals(QueueChannel.class.getName(), instance.getClassName());
	}

	@Test
	public void eventDrivenConsumerRegistered() throws Exception {
		context.registerBeanDefinition("testChannel", new RootBeanDefinition(DirectChannel.class));
		RootBeanDefinition endpointDef = new RootBeanDefinition(EventDrivenConsumer.class);
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("testChannel"));
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new BridgeHandler());
		context.registerBeanDefinition("eventDrivenConsumer", endpointDef);
		BeanDefinition controlBusDef = new RootBeanDefinition(ControlBus.class);
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("mbeanServer"));
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue("domain.test3");
		context.registerBeanDefinition("controlBus", controlBusDef);
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(
				ObjectNameManager.getInstance("domain.test3:type=endpoint,name=eventDrivenConsumer"));
		assertEquals(EventDrivenConsumer.class.getName(), instance.getClassName());
	}

	@Test
	public void pollingConsumerRegistered() throws Exception {
		context.registerBeanDefinition("testChannel", new RootBeanDefinition(QueueChannel.class));
		RootBeanDefinition endpointDef = new RootBeanDefinition(PollingConsumer.class);
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("testChannel"));
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new BridgeHandler());
		endpointDef.getPropertyValues().add("trigger", new PeriodicTrigger(10000));
		context.registerBeanDefinition("pollingConsumer", endpointDef);
		context.registerBeanDefinition("taskScheduler", new RootBeanDefinition(ThreadPoolTaskScheduler.class));
		BeanDefinition controlBusDef = new RootBeanDefinition(ControlBus.class);
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("mbeanServer"));
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue("domain.test4");
		context.registerBeanDefinition("controlBus", controlBusDef);
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(
				ObjectNameManager.getInstance("domain.test4:type=endpoint,name=pollingConsumer"));
		assertEquals(PollingConsumer.class.getName(), instance.getClassName());
	}

	@Test
	public void channelMonitoring() throws Exception {
		RootBeanDefinition channelDef = new RootBeanDefinition(DirectChannel.class);
		context.registerBeanDefinition("testChannel", channelDef);
		RootBeanDefinition endpointDef = new RootBeanDefinition(EventDrivenConsumer.class);
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("testChannel"));
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new MessageHandler() {
			private final AtomicInteger count = new AtomicInteger();
			public void handleMessage(Message<?> message) {
				int current = count.incrementAndGet();
				if (current % 10 == 0) {
					throw new RuntimeException("intentional test failure");
				}
			}
		});
		context.registerBeanDefinition("testEndpoint", endpointDef);
		BeanDefinition controlBusDef = new RootBeanDefinition(ControlBus.class);
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("mbeanServer"));
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue("domain.channel.monitor");
		context.registerBeanDefinition("controlBus", controlBusDef);
		context.refresh();
		MessageChannel channel = context.getBean("testChannel", MessageChannel.class);
		for (int i = 0; i < 100; i++) {
			try {
				channel.send(new GenericMessage<String>("foo"));
			}
			catch (Exception e) {
				// ignore
			}
		}
		MBeanServer server = context.getBean("mbeanServer", MBeanServer.class);
		ObjectName objectName = ObjectNameManager.getInstance("domain.channel.monitor:type=channel,name=testChannel");
		assertEquals(90L, server.getAttribute(objectName, "SendSuccessCount"));
		assertEquals(10L, server.getAttribute(objectName, "SendErrorCount"));
	}

}
