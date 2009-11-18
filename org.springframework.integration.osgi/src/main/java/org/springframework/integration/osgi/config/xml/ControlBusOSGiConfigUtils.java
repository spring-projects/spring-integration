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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.integration.controlbus.ControlBus;
import org.springframework.integration.osgi.IntegrationOSGiConstants;
import org.springframework.integration.osgi.extender.ControlBusBindingMessageDistributionListener;
import org.springframework.integration.osgi.extender.ControlBusListeningDecorator;
import org.springframework.integration.osgi.extender.ControlBusRegistrationMessageDistributionListener;
import org.springframework.osgi.service.exporter.support.AutoExport;
import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;
import org.springframework.osgi.service.importer.support.Cardinality;
import org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper class used by varilus bean parsers to create OSGi Factory bean definitions
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class ControlBusOSGiConfigUtils {
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
			defineServiceImporterFor(busGroupName, filter, registry, ControlBus.class);
		
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
	/**
	 * Will define OSGi Service exporter BeanDefinitionBuilder for a bean identified by the 'beanName' 
	 * parameter as an OSGi Service. If 'publishInterfaces' parameter is not provided this exporter will 
	 * use all interfaces implemented by this service class hierarchy. Otherwise you can provide an array of 
	 * specific interfaces to be published under this service.
	 * 
	 * @param beanName
	 * @param registry
	 * @param publishedIntefaces
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static BeanDefinitionBuilder defineServiceExporterFor(String beanName, 
			                                                     BeanDefinitionRegistry registry, 
			                                                     Class... publishedIntefaces){
		BeanDefinitionBuilder serviceBuilder = BeanDefinitionBuilder.genericBeanDefinition(OsgiServiceFactoryBean.class);
		serviceBuilder.addPropertyValue("targetBeanName", beanName);
		if (publishedIntefaces != null && publishedIntefaces.length > 0){
			serviceBuilder.addPropertyValue("interfaces", publishedIntefaces);
		} else {
			serviceBuilder.addPropertyValue("autoExport", AutoExport.INTERFACES);
		}
		serviceBuilder.addPropertyValue("registerService", true);
		return serviceBuilder;
	}
	/**
	 * 
	 * @param beanName
	 * @param filter
	 * @param registry
	 * @param publishedIntefaces
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static BeanDefinitionBuilder defineServiceImporterFor(String beanName, String filter, 
			                                                     BeanDefinitionRegistry registry, Class... publishedIntefaces){
		Assert.notEmpty(publishedIntefaces, "At least one interface must be provided");
		BeanDefinitionBuilder serviceBuilder = BeanDefinitionBuilder.genericBeanDefinition(OsgiServiceProxyFactoryBean.class);
		serviceBuilder.addPropertyValue("cardinality", Cardinality.C_0__1);
		if (StringUtils.hasText(filter)){
			serviceBuilder.addPropertyValue("filter", filter);
		}
		if (publishedIntefaces != null && publishedIntefaces.length > 0){
			serviceBuilder.addPropertyValue("interfaces", publishedIntefaces);
		} 
		// will make sure it uses exporter's bean name when building a relationship with importer
		serviceBuilder.addPropertyValue("serviceBeanName", beanName);
	
		//TODO: pf.setTimeout(timeoutInMillis)

		return serviceBuilder;
	}
	/**
	 * Will define a registration listener bean definition for the exported service adding reference to the 
	 * to the ControlBusListeninigDecorator to it.
	 * 
	 * @param registry
	 * @param exporterBuilder
	 * @param busBeanName
	 * @return
	 */
	public static AbstractBeanDefinition defineRegistrationMessageDistributor(BeanDefinitionRegistry registry,
											         BeanDefinitionBuilder exporterBuilder, 
											         String busBeanName){
		// create listener builder
		BeanDefinitionBuilder listenerBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition(ControlBusRegistrationMessageDistributionListener.class);
		String busGroupName = busBeanName;
		// if reference to the bus doesn't exist yet, create one
		// corresponding bean is not the actual bus but a ControlBusListentingDecorator
		if (!registry.containsBeanDefinition(busGroupName)){
			ControlBusOSGiConfigUtils.registerImporterForControlBus(registry, busGroupName);
		}	
		listenerBuilder.addConstructorArgReference(busGroupName);
		AbstractBeanDefinition listenerDefinition = listenerBuilder.getBeanDefinition();
		//exporterBuilder.addPropertyValue("listeners", listenerDefinition);
		return listenerDefinition;
	}
	/**
	 * 
	 * @param registry
	 * @param exporterBuilder
	 * @param busBeanName
	 * @return
	 */
	public static AbstractBeanDefinition defineBindingMessageDistributor(BeanDefinitionRegistry registry,
	         BeanDefinitionBuilder exporterBuilder, 
	         String busBeanName){
		// create listener builder
		BeanDefinitionBuilder listenerBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition(ControlBusBindingMessageDistributionListener.class);
		String busGroupName = busBeanName;
		// if reference to the bus doesn't exist yet, create one
		// corresponding bean is not the actual bus but a ControlBusListentingDecorator
		if (!registry.containsBeanDefinition(busGroupName)){
			ControlBusOSGiConfigUtils.registerImporterForControlBus(registry, busGroupName);
		}	
		listenerBuilder.addConstructorArgReference(busGroupName);
		AbstractBeanDefinition listenerDefinition = listenerBuilder.getBeanDefinition();
		return listenerDefinition;
	}
	
	public static List<String> discoverDependentElements(Element element, String attributeValue){
		List<String> dependentSources = new ArrayList<String>();
		Element documentElement = element.getOwnerDocument().getDocumentElement();
		NodeList nodes = documentElement.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE){
				Element elementNode = (Element) node;
				String elementName = isDependent(elementNode, attributeValue);
				if (StringUtils.hasText(elementName)){
					dependentSources.add(elementName);
				}
			}	
		}
		return dependentSources;
	}
	//
	private static String isDependent(Element element, String attributeValue){
		String[] monitoredAttributes = new String[]{"input-channel",
												 "output-channel",
												 "default-request-channel",
												 "default-reply-channel",
												 "channel"};
		for (String monitoredAttribute : monitoredAttributes) {
			if (element.hasAttribute(monitoredAttribute) &&
				element.getAttribute(monitoredAttribute).equals(attributeValue)	){
				if (!element.hasAttribute("name") && !element.hasAttribute("id")){
					// need to generate id for elements that do not have one
					// so they can be included in the listener dependentSource list
					element.setAttribute("id", element.getTagName() + "-" + attributeValue + "-" + element.hashCode());
					element.setIdAttribute("id", true);
				}
				String elementName = StringUtils.hasText(element.getAttribute("id")) ? 
									element.getAttribute("id") : element.getAttribute("name");
				return elementName;
			}
		}
		return null;
	}
}
