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
import org.springframework.osgi.service.ServiceUnavailableException;

/**
 * Tests 'config' element.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ConfigParserImporterTests extends AbstractSIConfigBundleTestDeployer {

	@Test
	public void testBasicSIServiceConfigNoBackingService() throws Exception {
		BundleContext bundleContext = SIBundleContextStub.getNewInstance();
		ApplicationContext ac = this.deploySIConfig(bundleContext, 
				                                    "org/springframework/integration/osgi/config/xml/", 
				                                    "ConfigParserImporterTests-default.xml");
		SubscribableChannel channel = ac.getBean("channelA", SubscribableChannel.class);
		assertNotNull(channel);
	}
	@Test(expected=ServiceUnavailableException.class)
	public void testBasicSIServiceConfigNoBackingServiceError() throws Exception {
		BundleContext bundleContext = SIBundleContextStub.getNewInstance();
		ApplicationContext ac = this.deploySIConfig(bundleContext, 
				                                    "org/springframework/integration/osgi/config/xml/", 
				                                    "ConfigParserImporterTests-default.xml");
		SubscribableChannel channel = ac.getBean("channelA", SubscribableChannel.class);
		assertNotNull(channel);
		// try to use it and see error
		channel.send(null);
	}
	/**
	 * Will register AC with service importer, then it will register another AC with service exporter
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBasicSIServiceConfigWithBackingService() throws Exception {
		BundleContext bundleContext = SIBundleContextStub.getNewInstance();
		ApplicationContext ac = this.deploySIConfig(bundleContext, 
				                                    "org/springframework/integration/osgi/config/xml/", 
				                                    "ConfigParserImporterTests-default.xml");
		SubscribableChannel channel = ac.getBean("channelA", SubscribableChannel.class);
		assertNotNull(channel);
		this.deploySIConfig(bundleContext, 
                "org/springframework/integration/osgi/config/xml/", 
                "ConfigParserImporterTests-exporter.xml");
		MessageHandler handler =  Mockito.mock(MessageHandler.class);
		Message<String> message = new StringMessage("hello");
		channel.subscribe(handler);
		channel.send(message);
		Mockito.verify(handler, Mockito.times(1)).handleMessage(message);
	}
	@Test
	public void testControlBusAttributeWithBusPresent() throws Exception {
		BundleContext bundleContext = SIBundleContextStub.getNewInstance();
		ConfigurableApplicationContext busAC = this.deploySIConfig(bundleContext, 
              "org/springframework/integration/osgi/config/xml/", 
              "BusConfigParserTests-default.xml");
		ConfigurableApplicationContext exporterAC = this.deploySIConfig(bundleContext, 
                "org/springframework/integration/osgi/config/xml/", 
                "ConfigParserImporterTests-exporter.xml");
		ConfigurableApplicationContext ac = this.deploySIConfig(bundleContext, 
				                                    "org/springframework/integration/osgi/config/xml/", 
				                                    "ConfigParserImporterTests-default.xml");
		assertNotNull(ac.getBean("channelA"));

		ControlBus bus = (ControlBus) ac.getBean("DEFAULT_CONTROL_GROUP");
		assertNotNull(bus);
		MessageHandler handler = Mockito.mock(MessageHandler.class);
		bus.subscribe(handler);
		exporterAC.close();
		// should be 3 notification messages sent to the bus
		Mockito.verify(handler, Mockito.times(1)).handleMessage((Message<?>) Mockito.any());
	}
}
