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
import org.springframework.osgi.service.importer.OsgiServiceLifecycleListener;

/**
 * TODO - insert COMMENT
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ControlBusBindingMessageDistributionListener implements
		OsgiServiceLifecycleListener {
	private static final Log log = LogFactory.getLog(ControlBusBindingMessageDistributionListener.class);
	private ControlBus controlBus;
	
	public ControlBusBindingMessageDistributionListener(ControlBus controlBus){
		this.controlBus = controlBus;
	}

	/* (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.OsgiServiceLifecycleListener#bind(java.lang.Object, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	public void bind(Object service, Map properties) throws Exception {
		if (controlBus.isBusAvailable()){
			log.info("Dispatching BINDING Message for: " + properties.get(IntegrationOSGiConstants.OSGI_BEAN_NAME) + 
					"- " + properties + " to: " + 
					controlBus.getName());
			MessageBuilder builder = MessageBuilder.withPayload(properties.get(IntegrationOSGiConstants.OSGI_BEAN_NAME));
			builder.copyHeaders(properties);
			builder.setHeader(IntegrationOSGiConstants.INTEGRATION_EVENT_TYPE, IntegrationOSGiConstants.BINDING);
			controlBus.send(builder.build());
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.OsgiServiceLifecycleListener#unbind(java.lang.Object, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	public void unbind(Object service, Map properties) throws Exception {
		if (controlBus.isBusAvailable()){
			log.info("Dispatching UNBINDING Message for: " + properties.get(IntegrationOSGiConstants.OSGI_BEAN_NAME) + 
					"- " + properties + " to: " + 
					controlBus.getName());
			MessageBuilder builder = MessageBuilder.withPayload(properties.get(IntegrationOSGiConstants.OSGI_BEAN_NAME));
			builder.copyHeaders(properties);
			builder.setHeader(IntegrationOSGiConstants.INTEGRATION_EVENT_TYPE, IntegrationOSGiConstants.UNBINDING);
			controlBus.send(builder.build());
		}
	}

}
