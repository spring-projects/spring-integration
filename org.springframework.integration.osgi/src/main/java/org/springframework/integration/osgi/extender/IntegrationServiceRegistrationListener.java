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
import org.springframework.integration.message.StringMessage;
import org.springframework.osgi.service.exporter.OsgiServiceRegistrationListener;

/**
 * Service Registration listener which publishes registration life-cycle Messages 
 * to the {@link ControlBus}
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class IntegrationServiceRegistrationListener implements OsgiServiceRegistrationListener {
	private static final Log log = LogFactory.getLog(IntegrationServiceRegistrationListener.class);
	private ControlBusListeningDecorator controlBusDecorator;
	
	public IntegrationServiceRegistrationListener(ControlBusListeningDecorator controlBusDecorator){
		this.controlBusDecorator = controlBusDecorator;
	}
	/**
	 * Will send a notification message to the named {@link ControlBus} notifying that service
	 * for an SI component was registered and is ready to be used. 
	 */
	@SuppressWarnings("unchecked")
	public void registered(Object service, Map properties){
		if (controlBusDecorator.isBusAvailable()){
			log.info("Dispatching REGISTRATION Message for: " + service + "- " + properties + " to: " + 
					controlBusDecorator.getName());
			//TODO: change to structural message 
			controlBusDecorator.send(new StringMessage("Dispatching REGISTRATION Message for: " + service + "- " + properties));
		}
	}
	/**
	 * Will send a notification message to the named {@link ControlBus} notifying that service
	 * for an SI component was un-registered and can no longet be used.
	 */
	@SuppressWarnings("unchecked")
	public void unregistered(Object service, Map properties){
		if (controlBusDecorator.isBusAvailable()){
			log.info("Dispatching UN-REGISTRATION Message for: " + service + "- " + properties + " to: " + 
					controlBusDecorator.getName());
			//TODO: change to structural message 
			controlBusDecorator.send(new StringMessage("Dispatching UN-REGISTRATION Message for: " + service + "- " + properties));
		}
	}
}
