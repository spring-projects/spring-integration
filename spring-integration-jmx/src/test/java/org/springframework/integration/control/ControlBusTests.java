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
import static org.junit.Assert.assertNotNull;

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
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.monitor.LifecycleMessageHandlerMetrics;
import org.springframework.integration.monitor.QueueChannelMetrics;
import org.springframework.integration.monitor.DirectChannelMetrics;
import org.springframework.integration.scheduling.PollerMetadata;
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
		registerControlBus(context, "domain.test1");
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(ObjectNameManager
				.getInstance("domain.test1:type=MessageChannel,name=directChannel"));
		assertEquals(DirectChannelMetrics.class.getName(), instance.getClassName());
	}

	@Test
	public void anonymousDirectChannelRegistered() throws Exception {
		context.registerBeanDefinition("org.springframework.integration.generated#0", new RootBeanDefinition(
				DirectChannel.class));
		registerControlBus(context, "domain.test1b");
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer
				.getObjectInstance(ObjectNameManager
						.getInstance("domain.test1b:type=MessageChannel,name=org.springframework.integration.generated#0,source=anonymous"));
		assertEquals(DirectChannelMetrics.class.getName(), instance.getClassName());
	}

	@Test
	public void staticObjectNamePropertiesRegistered() throws Exception {
		context.registerBeanDefinition("directChannel", new RootBeanDefinition(DirectChannel.class));
		BeanDefinition exporterDef = registerControlBus(context, "domain.test1a");
		exporterDef.getPropertyValues().add("objectNameStaticProperties", Collections.singletonMap("foo", "bar"));
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(ObjectNameManager
				.getInstance("domain.test1a:type=MessageChannel,name=directChannel,foo=bar"));
		assertEquals(DirectChannelMetrics.class.getName(), instance.getClassName());
	}

	@Test
	public void queueChannelRegistered() throws Exception {
		context.registerBeanDefinition("queueChannel", new RootBeanDefinition(QueueChannel.class));
		registerControlBus(context, "domain.test2");
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(ObjectNameManager
				.getInstance("domain.test2:type=MessageChannel,name=queueChannel"));
		assertEquals(QueueChannelMetrics.class.getName(), instance.getClassName());
	}

	@Test
	public void eventDrivenConsumerRegistered() throws Exception {
		context.registerBeanDefinition("testChannel", new RootBeanDefinition(DirectChannel.class));
		RootBeanDefinition endpointDef = new RootBeanDefinition(EventDrivenConsumer.class);
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("testChannel"));
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new RootBeanDefinition(BridgeHandler.class));
		context.registerBeanDefinition("eventDrivenConsumer", endpointDef);
		registerControlBus(context, "domain.test3");
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(ObjectNameManager
				.getInstance("domain.test3:type=MessageHandler,name=eventDrivenConsumer,bean=endpoint"));
		assertEquals(LifecycleMessageHandlerMetrics.class.getName(), instance.getClassName());
	}

	@Test
	public void pollingConsumerRegistered() throws Exception {
		context.registerBeanDefinition("testChannel", new RootBeanDefinition(QueueChannel.class));
		RootBeanDefinition endpointDef = new RootBeanDefinition(PollingConsumer.class);
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("testChannel"));
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new RootBeanDefinition(BridgeHandler.class));
		RootBeanDefinition pollerMetaDefinition = new RootBeanDefinition(PollerMetadata.class);
		pollerMetaDefinition.getPropertyValues().add("trigger", new PeriodicTrigger(10000));
		endpointDef.getPropertyValues().add("pollerMetadata", pollerMetaDefinition);
		context.registerBeanDefinition("pollingConsumer", endpointDef);
		context.registerBeanDefinition("taskScheduler", new RootBeanDefinition(ThreadPoolTaskScheduler.class));
		registerControlBus(context, "domain.test4");
		context.refresh();
		MBeanServer mbeanServer = context.getBean("mbeanServer", MBeanServer.class);
		ObjectInstance instance = mbeanServer.getObjectInstance(ObjectNameManager
				.getInstance("domain.test4:type=MessageHandler,name=pollingConsumer,bean=endpoint"));
		assertEquals(LifecycleMessageHandlerMetrics.class.getName(), instance.getClassName());
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
		registerControlBus(context, "domain.channel.monitor");
		context.refresh();
		MBeanServer server = context.getBean("mbeanServer", MBeanServer.class);
		ObjectName objectName = ObjectNameManager.getInstance("domain.channel.monitor:type=MessageChannel,name=testChannel");
		assertNotNull(server.getObjectInstance(objectName));
	}

	private BeanDefinition registerControlBus(GenericApplicationContext context, String domain) {
		BeanDefinition exporterDef = new RootBeanDefinition(IntegrationMBeanExporter.class);
		exporterDef.getPropertyValues().addPropertyValue("server", new RuntimeBeanReference("mbeanServer"));
		exporterDef.getPropertyValues().addPropertyValue("defaultDomain", domain);
		context.registerBeanDefinition("exporter", exporterDef);
		BeanDefinition controlBusDef = new RootBeanDefinition(ControlBus.class);
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("mbeanServer"));
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("exporter"));
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new DirectChannel());
		context.registerBeanDefinition("controlBus", controlBusDef);
		return exporterDef;
	}

}
