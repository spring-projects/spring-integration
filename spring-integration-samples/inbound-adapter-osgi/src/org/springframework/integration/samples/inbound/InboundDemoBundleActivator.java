/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.integration.samples.inbound;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
/**
 * Simple BundleActivator which will register ServiceListener which will listen for 
 * ApplicationContext published event. Once event is received, HelloWorldDemo will be executed.
 * 
 * @author Oleg Zhurakousky
 */
public class InboundDemoBundleActivator implements BundleActivator, CommandProvider, ServiceListener {
	private BundleContext context;
	private InboundGateway gateway;
	
	
	public void start(BundleContext context) throws Exception {
		this.context = context;
		this.context.addServiceListener(this);
		Dictionary props = new Hashtable();
		context.registerService(CommandProvider.class.getName(), this, props);
	}
	/**
	 * 
	 */
	public void stop(BundleContext arg0) throws Exception {}
	/**
	 * 
	 */
	public String getHelp() {
		return "\n### Spring Integration CLI-based Demo\n" +
				"siSend - will send a message to a reciever which will write the message to a file\n\t" +
				"siSend <message> <file name> Example: siSend hello foo.txt\n";
	}
	/**
	 * 
	 */
	public void serviceChanged(ServiceEvent serviceEvent) {
		ServiceReference sr = serviceEvent.getServiceReference();
		if (context.getBundle().getSymbolicName().equals(sr.getProperty(Constants.BUNDLE_SYMBOLICNAME))){
			ApplicationContext applicationContext = (ApplicationContext) context.getService(sr);
			gateway = (InboundGateway) applicationContext.getBean("inboundGateway");
		}	
	}
	/**
	 * 
	 * @param ci
	 */
	public void _siSend(CommandInterpreter ci){	
		String message = ci.nextArgument();
		String fileName = ci.nextArgument();
		Assert.notNull(message, "You must provide message as first argument");
		Assert.notNull(fileName, "You must proivi a file name as second argument");
		ci.println("Sending message: '" + message + "'");
		File file = gateway.sendMessage(message, fileName);
		ci.println("Message sent and its contents are written to: " + file.getAbsolutePath());
	}

}
