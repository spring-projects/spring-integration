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
package org.springframework.integration.osgi.demo;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.osgi.AbstractSIConfigBundleTestDeployer;
import org.springframework.integration.osgi.stubs.SIBundleContextStub;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

/**
 * Simple demo shows how Spring-DM namespace support could be applied without using 
 * real OSGi runtime, by simply stubbing out OSGi BundleContext and passing it to 
 * OsgiBundleXmlApplicationContext
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SpringDMnoOSGiDemo extends AbstractSIConfigBundleTestDeployer {
	
	public static void main(String[] args) throws Exception {
		new SpringDMnoOSGiDemo().runDemo();
	}
	/**
	 * 
	 */
	public void runDemo() throws Exception {
		BundleContext bundleContext = SIBundleContextStub.getNewInstance();
		ApplicationContext serviceExporterAC = this.deploySIConfig(bundleContext, 
				                "org/springframework/integration/osgi/extender/", 
				                "ac-service-exporter.xml");
		ApplicationContext serviceImporterAC = this.deploySIConfig(bundleContext, 
                                "org/springframework/integration/osgi/extender/", 
                                "ac-service-importer.xml");
		// get channel from the exporter AC and send message
		SubscribableChannel channel = serviceExporterAC.getBean("input", SubscribableChannel.class);
		channel.send(new StringMessage("Hello String-DM without OSGi"));
		// the output coming out of Service Activator defined in importer AC should display the message
	}
	
}
