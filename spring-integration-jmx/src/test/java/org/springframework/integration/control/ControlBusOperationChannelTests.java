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

import javax.management.MBeanServer;

import org.junit.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.jmx.JmxHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.support.JmxUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class ControlBusOperationChannelTests {

	private final MBeanServer server = JmxUtils.locateMBeanServer();

	private final String domain = "domain.test";


	@Test
	public void replyProducingOperation() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		RootBeanDefinition endpointDef = new RootBeanDefinition(EventDrivenConsumer.class);
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new DirectChannel());
		endpointDef.getConstructorArgumentValues().addGenericArgumentValue(new TestHandler());
		context.registerBeanDefinition("testEndpoint", endpointDef);
		RootBeanDefinition busDef = new RootBeanDefinition(ControlBus.class);
		busDef.getConstructorArgumentValues().addGenericArgumentValue(server);
		busDef.getConstructorArgumentValues().addGenericArgumentValue(domain);
		context.registerBeanDefinition("controlBus", busDef);
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
		context.close();
	}


	private static class TestHandler implements MessageHandler {
		public void handleMessage(Message<?> message) {
		}
	}

}
