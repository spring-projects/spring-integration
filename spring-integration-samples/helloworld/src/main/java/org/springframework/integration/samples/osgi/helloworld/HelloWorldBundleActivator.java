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

package org.springframework.integration.samples.osgi.helloworld;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.samples.helloworld.HelloWorldDemo;

/**
 * An OSGi BundleActivator which will register a ServiceListener to listen
 * for an  ApplicationContext published event. Once the event is received,
 * the HelloWorldDemo will be executed.
 * 
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
public class HelloWorldBundleActivator implements BundleActivator, ServiceListener{

	private BundleContext context;

	public void start(BundleContext context) throws Exception {
		this.context = context;		
		context.addServiceListener(this);
	}

	public void stop(BundleContext context) throws Exception {}

	public void serviceChanged(ServiceEvent serviceEvent) {
		ServiceReference sr = serviceEvent.getServiceReference();
		if (context.getBundle().getSymbolicName().equals(sr.getProperty(Constants.BUNDLE_SYMBOLICNAME))) {
			ApplicationContext applicationContext = (ApplicationContext) context.getService(sr);
			new HelloWorldDemo().performDemo(applicationContext);
		}
	}

}
