/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.integration.osgi.config.xml;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.controlbus.ControlBus;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.osgi.AbstractSIConfigBundleTestDeployer;
import org.springframework.integration.osgi.stubs.SIBundleContextStub;

/**
 * Tests 'config' element.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ConfigParserExporterTests extends AbstractSIConfigBundleTestDeployer {

	@Test
	public void testBasicSIServiceConfig() throws Exception {
		BundleContext bundleContext = SIBundleContextStub.getNewInstance();
		ApplicationContext ac = this.deploySIConfig(bundleContext, 
				                                    "org/springframework/integration/osgi/config/xml/", 
				                                    "ConfigParserExporterTests-default.xml");
		assertNotNull(ac.getBean("channelA-Service"));
		assertNotNull(ac.getBean("channelB-Service"));
		assertNotNull(ac.getBean("channelC-Service"));
		
		SubscribableChannel channel = (SubscribableChannel) ac.getBean("channelA");
		SubscribableChannel channel_reference = ac.getBean("channelA-reference", SubscribableChannel.class);
		// verify that standar OSGi configuration can still bind to this service
		MessageHandler handler = Mockito.mock(MessageHandler.class);
		channel_reference.subscribe(handler);
		Message<String> message = new StringMessage("hello");
		channel.send(message);
		Mockito.verify(handler, Mockito.times(1)).handleMessage(message);
	}
	@Test
	public void testControlBusAttributeWithBusPresent() throws Exception {
		BundleContext bundleContext = SIBundleContextStub.getNewInstance();
		ConfigurableApplicationContext busAC = this.deploySIConfig(bundleContext, 
              "org/springframework/integration/osgi/config/xml/", 
              "BusConfigParserTests-default.xml");
		ConfigurableApplicationContext ac = this.deploySIConfig(bundleContext, 
				                                    "org/springframework/integration/osgi/config/xml/", 
				                                    "ConfigParserExporterTests-withbus-attribute.xml");
		assertNotNull(ac.getBean("channelA-Service"));
		assertNotNull(ac.getBean("channelB-Service"));
		assertNotNull(ac.getBean("channelC-Service"));
		ControlBus bus = (ControlBus) ac.getBean("DEFAULT_CONTROL_GROUP");
		assertNotNull(bus);
		MessageHandler handler = Mockito.mock(MessageHandler.class);
		bus.subscribe(handler);
		ac.close();
		// should be 3 notification messages sent to the bus
		Mockito.verify(handler, Mockito.times(3)).handleMessage((Message<?>) Mockito.any());
	}
}
