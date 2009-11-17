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
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.osgi.service.importer.OsgiServiceLifecycleListener;

/**
 * Wraps OSGi Service Reference representing a {@link ControlBus} to provide several simplification 
 * around life-cycle of the underlying reference.
 * 
 * First, this wrapper implements {@link OsgiServiceLifecycleListener} which will listen for 
 * bind/unbind events of the service reference representing a particular instance of the {@link ControlBus} service.
 * Calls to bind/unbind methods will set <code>busAvailable</code> attribute to 'true'/'false' respectively.
 * sparing you from dealing with the internals of the service reference proxy, which might be at the state where
 * it is not backed up by a concrete instance of the {@link ControlBus} service (e.g., when such service is unregistered). 
 * This class also implements {@link ControlBus} interface delegating all method calls to the actual instance of the ControlBus
 * with one exception. Call to {@link ControlBus#isBusAvailable()} will return the value of the <code>busAvailable</code>
 * attribute which is set by the calls to bind/unbind methods.
 * This way you can interact with the ControlBus as if it was a real instance of the ControlBus service when it is 
 * available, but it also give you a quick and convenient way to know when bus goes away. 
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@SuppressWarnings("unchecked")
public class ControlBusListeningDecorator implements OsgiServiceLifecycleListener, ControlBus {

	private static final Log log = LogFactory.getLog(ControlBusListeningDecorator.class);
	private boolean busAvailable;
	private ControlBus controlBus;
	private String name;
	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.OsgiServiceLifecycleListener#bind(java.lang.Object, java.util.Map)
	 */
	public void bind(Object service, Map properties) throws Exception {
		controlBus = (ControlBus) service;
		name = controlBus.getName();
		log.debug("Binding to the Control Bus: " + name);
		busAvailable = true;
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.OsgiServiceLifecycleListener#unbind(java.lang.Object, java.util.Map)
	 */
	public void unbind(Object service, Map properties) throws Exception {
		log.debug("un-Binding from the Control Bus: " + name);
		busAvailable = false;
		controlBus = null;
	}
	/*
	 */
	public void setControlBus(ControlBus controlBus) {
		this.controlBus = controlBus;
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.controlbus.ControlBus#isBusAvailable()
	 */
	public boolean isBusAvailable() {
		return busAvailable;
	}
	/*
	 */
	public void setBusAvailable(boolean busAvailable) {
		this.busAvailable = busAvailable;
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.channel.SubscribableChannel#subscribe(org.springframework.integration.message.MessageHandler)
	 */
	public boolean subscribe(MessageHandler handler) {
		if (isBusAvailable()){
			return controlBus.subscribe(handler);
		} else {
			log.debug("Can not subscribe to the Control Bus: " + name + ". Control Bus is not availabel");
			return false;
		}
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.channel.SubscribableChannel#unsubscribe(org.springframework.integration.message.MessageHandler)
	 */
	public boolean unsubscribe(MessageHandler handler) {
		if (isBusAvailable()){
			return controlBus.unsubscribe(handler);
		} else {
			log.debug("Can not unsubscribe from the Control Bus: " + name + ". Control Bus is not availabel");
			return false;
		}
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.core.MessageChannel#getName()
	 */
	public String getName() {
		return name;
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.core.MessageChannel#send(org.springframework.integration.core.Message)
	 */
	public boolean send(Message<?> message) {
		if (isBusAvailable()){
			return controlBus.send(message);
		} else {
			log.debug("Can not send message to the Control Bus: " + name + ". Control Bus is not availabel");
			return false;
		}
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.core.MessageChannel#send(org.springframework.integration.core.Message, long)
	 */
	public boolean send(Message<?> message, long timeout) {
		if (isBusAvailable()){
			return controlBus.send(message, timeout);
		} else {
			log.debug("Can not send message to the Control Bus: " + name + ". Control Bus is not availabel");
			return false;
		}
	}
}
