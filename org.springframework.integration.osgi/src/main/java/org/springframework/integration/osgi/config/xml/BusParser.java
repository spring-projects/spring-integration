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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.controlbus.ControlBus;
import org.springframework.integration.osgi.extender.IntegrationServiceRegistrationListener;
import org.springframework.intergration.osgi.ControlBusAwarePostProcessor;
import org.springframework.intergration.osgi.IntegrationOSGiConstants;
import org.springframework.osgi.config.internal.adapter.OsgiServiceRegistrationListenerAdapter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser to process 'bus' element 
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class BusParser extends AbstractBeanDefinitionParser {
	
	private static final Log log = LogFactory.getLog(BusConfigParser.class);
	private String busGroupName;
	/**
	 * 
	 */
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		return "controlBusPostProcessor-" + busGroupName;
	}
	/**
	 * Will parse 'bus' element and its sub elements
	 */
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		this.setBusGroupName(element);
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		BeanDefinitionBuilder rootDefinition = BeanDefinitionBuilder.rootBeanDefinition(ControlBusAwarePostProcessor.class);
		
		List<Element> producerList = DomUtils.getChildElementsByTagName(element, "manage-producer");
		//List<Element> consumerList = DomUtils.getChildElementsByTagName(element, "manage-consumer");
		
		for (Element producerElement : producerList) {
			String producerName = producerElement.getAttribute("ref");
			Assert.isTrue(StringUtils.hasText(producerName), "'manage-producer' must define 'ref' attribute");
			// define service exporter 
			BeanDefinitionBuilder serviceExportBuilder =  
				AbstractOSGiServiceManagingParserUtil.defineServiceExporterFor(producerName, registry);
			
			String registrationListenerName = this.defineRegistrationListener(registry);
			
			// add listener(s) to the service exporter
			serviceExportBuilder.addPropertyReference("listeners", registrationListenerName);
			// register service exporter
			BeanDefinitionReaderUtils.registerWithGeneratedName(serviceExportBuilder.getBeanDefinition(), registry);
		}
		log.trace("Managed configuration for " + busGroupName + " was parsed successfully");
		return rootDefinition.getBeanDefinition();
	}
	/**
	 * Defines and registers OSGi Service Registration listener for the exported service (e.g., channel), which
	 * will communicate life-cycle events of this service to the Control Bus
	 */
	private String defineRegistrationListener(BeanDefinitionRegistry registry){
		// define POJO listener
		BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder.genericBeanDefinition(IntegrationServiceRegistrationListener.class);
		// inject Control Bus service instance into the listener
		BeanDefinition controlBusDefinition = this.registerImportedControlBusServiceDefinition(registry);
		listenerBuilder.addPropertyValue("controlBus", controlBusDefinition);
		String listenerName = 
			BeanDefinitionReaderUtils.registerWithGeneratedName(listenerBuilder.getBeanDefinition(), registry);
		// define listener adapter for the above listener
		BeanDefinitionBuilder listenerAdapterBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition(OsgiServiceRegistrationListenerAdapter.class);
		listenerAdapterBuilder.addPropertyValue("targetBeanName", listenerName);
		listenerAdapterBuilder.addPropertyValue("registrationMethod", "register");
		listenerAdapterBuilder.addPropertyValue("unregistrationMethod", "unRegister");		
		String registrationListenerName = 
			BeanDefinitionReaderUtils.registerWithGeneratedName(listenerAdapterBuilder.getBeanDefinition(), registry);
		return registrationListenerName;
	}
	/**
	 * Will define a service importer (equivalent to <osgi:reference>) for the ControlBus service.
	 */
	private BeanDefinition registerImportedControlBusServiceDefinition(BeanDefinitionRegistry registry){
		String filter = "(&(" + IntegrationOSGiConstants.OSGI_BEAN_NAME + "=" + busGroupName + "))";
		BeanDefinitionBuilder controlBusImporterBuilder = 
			AbstractOSGiServiceManagingParserUtil.defineServiceImporterFor(busGroupName, filter, registry, ControlBus.class);
		BeanDefinition controlBusImporterDefinition = controlBusImporterBuilder.getBeanDefinition();
		BeanDefinitionReaderUtils.registerWithGeneratedName(controlBusImporterBuilder.getBeanDefinition(), registry);
		return controlBusImporterDefinition;
	}
	/**
	 * Determines and sets the 'group-name' for the ControlBus group which will be managing this deployment
	 */
	private void setBusGroupName(Element element){
		busGroupName = element.getAttribute("group-name");
		Assert.isTrue(StringUtils.hasText(busGroupName), "bus-config 'group-name' attribute must be provided");
		log.debug("Control Bus group name: " + busGroupName);
	}
}
