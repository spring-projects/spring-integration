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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.jmx.JmxHeaders;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.support.MBeanServerFactoryBean;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class ControlBusOperationChannelTests {

	private final String domain = "domain.test";
	private GenericApplicationContext context;


	@Test
	public void replyProducingOperation() throws Exception {
		context = new GenericApplicationContext();
		RootBeanDefinition endpointDef = new RootBeanDefinition(EventDrivenConsumer.class);
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new DirectChannel());
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new RootBeanDefinition(TestHandler.class));
		context.registerBeanDefinition("testEndpoint", endpointDef);
		registerControlBus(context, domain);
		context.refresh();
		ControlBus controlBus = context.getBean("controlBus", ControlBus.class);
		EventDrivenConsumer endpoint = context.getBean("testEndpoint", EventDrivenConsumer.class);
		Message<?> stopMessage = MessageBuilder.withPayload("test")
				.setHeader(ControlBus.TARGET_BEAN_NAME, "testEndpoint")
				.setHeader(JmxHeaders.OPERATION_NAME, "stop")
				.build();
		Message<?> startMessage = MessageBuilder.withPayload("test")
				.setHeader(ControlBus.TARGET_BEAN_NAME, "testEndpoint")
				.setHeader(JmxHeaders.OPERATION_NAME, "start")
				.build();
		assertTrue("endpoint should be running after startup", endpoint.isRunning());
		controlBus.getOperationChannel().send(stopMessage);
		assertFalse("endpoint should have been stopped", endpoint.isRunning());
		controlBus.getOperationChannel().send(startMessage);
		assertTrue("endpoint should be running after being restarted", endpoint.isRunning());
		close();
	}

	@After
	public void close() {
		context.close();
	}

	private BeanDefinition registerControlBus(GenericApplicationContext context, String domain) {
		RootBeanDefinition serverDef = new RootBeanDefinition(MBeanServerFactoryBean.class);
		serverDef.getPropertyValues().add("locateExistingServerIfPossible", true);
		context.registerBeanDefinition("mbeanServer", serverDef);
		BeanDefinition exporterDef = new RootBeanDefinition(IntegrationMBeanExporter.class);
		exporterDef.getPropertyValues().addPropertyValue("server", new RuntimeBeanReference("mbeanServer"));
		exporterDef.getPropertyValues().addPropertyValue("domain", domain);
		context.registerBeanDefinition("exporter", exporterDef);
		BeanDefinition controlBusDef = new RootBeanDefinition(ControlBus.class);
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("mbeanServer"));
		controlBusDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("exporter"));
		context.registerBeanDefinition("controlBus", controlBusDef);
		return exporterDef;
	}

	private static class TestHandler implements MessageHandler {
		public void handleMessage(Message<?> message) {
		}
	}

}
