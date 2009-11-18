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
package org.springframework.integration.osgi.extender;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.controlbus.ControlBus;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.osgi.IntegrationOSGiConstants;
import org.springframework.osgi.service.exporter.OsgiServiceRegistrationListener;

/**
 * Will send control messages to the ControlBus when OSGi services are registered.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ControlBusRegistrationMessageDistributionListener implements OsgiServiceRegistrationListener {
	private static final Log log = LogFactory.getLog(ControlBusRegistrationMessageDistributionListener.class);
	private ControlBus controlBus;
	
	public ControlBusRegistrationMessageDistributionListener(ControlBus controlBus){
		this.controlBus = controlBus;
	}
	/**
	 * Will send a notification message to the named {@link ControlBus} notifying that service
	 * for an SI component was registered and is ready to be used. 
	 */
	@SuppressWarnings("unchecked")
	public void registered(Object service, Map properties){
		if (controlBus.isBusAvailable()){
			log.info("Dispatching REGISTRATION Message for: " + service + "- " + properties + " to: " + 
					controlBus.getName());
			MessageBuilder builder = MessageBuilder.withPayload(service);
			builder.copyHeaders(properties);
			builder.setHeader(IntegrationOSGiConstants.INTEGRATION_EVENT_TYPE, IntegrationOSGiConstants.REGISTRATION);
			controlBus.send(builder.build());
		}
	}
	/**
	 * Will send a notification message to the named {@link ControlBus} notifying that service
	 * for an SI component was un-registered and can no longet be used.
	 */
	@SuppressWarnings("unchecked")
	public void unregistered(Object service, Map properties){
		if (controlBus.isBusAvailable()){
			log.info("Dispatching UN-REGISTRATION Message for: " + service + "- " + properties + " to: " + 
					controlBus.getName());
			MessageBuilder builder = MessageBuilder.withPayload(service);
			builder.copyHeaders(properties);
			builder.setHeader(IntegrationOSGiConstants.INTEGRATION_EVENT_TYPE, IntegrationOSGiConstants.UNREGISTRATION);
			controlBus.send(builder.build());
		}
	}
}
