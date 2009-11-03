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

import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.controlbus.ControlBus;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.osgi.AbstractSIConfigBundleTestDeployer;
import org.springframework.integration.osgi.stubs.SIBundleContextStub;

/**
 * THis test bootstraps two instances of the Control Bus, then shuts down one demonstrating and testing 
 * transparent fail-over to another instance of the Control Bus
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class BusUsageFailoverTests extends AbstractSIConfigBundleTestDeployer {	
	static MessageHandler handler = Mockito.mock(MessageHandler.class);
	
	@Test
	public void testDefaultControlBusConfig() throws Exception {
		BundleContext bundleContext = SIBundleContextStub.getInstance();

		ConfigurableApplicationContext primaryAc = this.deploySIConfig(bundleContext, 
				            "org/springframework/integration/osgi/config/xml/", 
				            "BusUsageFailoverTests-context-primary.xml");
		
		this.deploySIConfig(bundleContext, 
	                        "org/springframework/integration/osgi/config/xml/", 
	                        "BusUsageFailoverTests-context-secondary.xml");
		
		ApplicationContext userAC = this.deploySIConfig(bundleContext, 
                									"org/springframework/integration/osgi/config/xml/", 
                									"BusUsageFailoverReferenceTests-context.xml");
		
		ControlBus bus = (ControlBus) userAC.getBean("bus");
		StringMessage message = new StringMessage("hello");
		
		bus.send(message);
		Thread.sleep(200);
		Mockito.verify(handler, Mockito.times(1)).handleMessage(message);
		primaryAc.close(); // shut down primary Bus
			  
		Mockito.reset(handler);
		bus.send(message);
		Thread.sleep(200);
		Mockito.verify(handler, Mockito.times(1)).handleMessage(message);
	}
	
	public static class BusListener{
		public void bind(ControlBus bus, Map properties){
			bus.subscribe(handler);
		}
		public void unBind(ControlBus bus, Map properties){
		}
	}
}
