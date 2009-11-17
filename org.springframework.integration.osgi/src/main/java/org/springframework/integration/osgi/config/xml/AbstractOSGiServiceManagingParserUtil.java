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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.integration.osgi.extender.IntegrationServiceRegistrationListener;
import org.springframework.osgi.service.exporter.support.AutoExport;
import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;
import org.springframework.osgi.service.importer.support.Cardinality;
import org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Utility class which wraps Spring-DM factory bean creation for exporting and importing SI components as
 * OSGi services.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class AbstractOSGiServiceManagingParserUtil {

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
	public static AbstractBeanDefinition defineRegistrationListenerForBus(BeanDefinitionRegistry registry,
											         BeanDefinitionBuilder exporterBuilder, 
											         String busBeanName){
		BeanDefinitionBuilder listenerBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition(IntegrationServiceRegistrationListener.class);
		String busGroupName = busBeanName;
		if (!registry.containsBeanDefinition(busGroupName)){
			ControlBusOSGiUtils.registerImporterForControlBus(registry, busGroupName);
		}	
		listenerBuilder.addConstructorArgReference(busGroupName);
		AbstractBeanDefinition listenerDefinition = listenerBuilder.getBeanDefinition();
		exporterBuilder.addPropertyValue("listeners", listenerDefinition);
		return listenerDefinition;
	}
}
