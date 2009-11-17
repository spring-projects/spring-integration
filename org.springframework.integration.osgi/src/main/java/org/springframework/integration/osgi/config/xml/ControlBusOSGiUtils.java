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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.integration.controlbus.ControlBus;
import org.springframework.integration.osgi.IntegrationOSGiConstants;
import org.springframework.integration.osgi.extender.ControlBusListeningDecorator;

/**
 * TODO - insert COMMENT
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ControlBusOSGiUtils {
	public final static String DEFAULT_BUS_GROUP_NAME = "DEFAULT_CONTROL_GROUP";
	public final static String DEFAULT_CONTROL_DIST_CHANNEL = "DEFAULT_CONTROL_DIST_CHANNEL";
	/**
	 * Will define a service importer (equivalent to <osgi:reference>) for the ControlBus service.
	 * It will also register binding listener for this 'controlbus' reference which could be used 
	 * to react to the life-cycle events of this 'controlbus' reference.
	 * For example: Individual service reference listeners coiuld use it to determine when the bus went away and
	 * no control events need to be dispatched to the bus.
	 */
	public static BeanDefinition registerImporterForControlBus(BeanDefinitionRegistry registry, String busGroupName){
		String filter = "(&(" + IntegrationOSGiConstants.OSGI_BEAN_NAME + "=" + busGroupName + "))";
		BeanDefinitionBuilder controlBusImporterBuilder = 
			AbstractOSGiServiceManagingParserUtil.defineServiceImporterFor(busGroupName, filter, registry, ControlBus.class);
		
		BeanDefinitionBuilder busListenerBuilder = 
								BeanDefinitionBuilder.genericBeanDefinition(ControlBusListeningDecorator.class);
		AbstractBeanDefinition busListenerDefinition = busListenerBuilder.getBeanDefinition();
		//String listenerName = IntegrationOSGiConstants.BUS_LISTENER_PREFIX + busGroupName;
		String listenerName = busGroupName;
		registry.registerBeanDefinition(listenerName, busListenerDefinition);
		controlBusImporterBuilder.addPropertyReference("listeners", listenerName);
		BeanDefinition controlBusImporterDefinition = controlBusImporterBuilder.getBeanDefinition();
		BeanDefinitionReaderUtils.registerWithGeneratedName(controlBusImporterBuilder.getBeanDefinition(), registry);
		return controlBusImporterDefinition;
	}
}
