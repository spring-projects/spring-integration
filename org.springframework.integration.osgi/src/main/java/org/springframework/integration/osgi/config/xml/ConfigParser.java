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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.osgi.extender.ControlBusRegistrationMessageDistributionListener;
import org.springframework.integration.osgi.extender.LifecycleServiceManagingListener;
import org.springframework.util.Assert;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser to process 'config' element of SI Service Extender 
 * and its sub-elements
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ConfigParser implements BeanDefinitionParser {
	private static final Log log = LogFactory.getLog(ConfigParser.class);
	private static final String TYPE = "si-type";
	private static final String REF = "ref";
	private static final String NAME = "name";
	private static final String CONTROL_BUS = "control-bus";
	private static final String EXPORTER_SUFFIX = "-Service";
	private static final int EXPORT = 1;
	private static final int IMPORT = 2;
	/**
	 * Will parse 'config' element of "integration-service-extender' namespace and its sub-elements
	 */
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		List<Element> elements = DomUtils.getChildElementsByTagName(element, "export");
		for (Element exportElements : elements) {
			this.processExportElement(exportElements, parserContext, EXPORT);
		}
		elements = DomUtils.getChildElementsByTagName(element, "import");
		for (Element exportElements : elements) {
			this.processImportElement(exportElements, parserContext, IMPORT);
		}
		return null;
	}
	/**
	 */
	private void processExportElement(Element element, ParserContext parserContext, int configType){
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		String includeType = this.getIncludeType(element);
		Assert.notNull(includeType, "You must prvide one of the following: 'type' or 'ref' attribute wihin the 'include' element");
		if (includeType.equals(TYPE)){
			this.processTypeAttribute(element, registry, configType);
		} else if (includeType.equals(REF)){
			this.processRefAttribute(element, registry, configType);
		}
	}
	/**
	 */
	private void processImportElement(Element element, ParserContext parserContext, int configType){
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		String serviceName = element.getAttribute(NAME);
		Assert.hasText(serviceName, "you must enter a valid value in the " + NAME + " attribute");
		Class[] interfaces = this.discoverInterfaces(serviceName, element);
		BeanDefinitionBuilder importerBuilder = 
			ControlBusOSGiConfigUtils.defineServiceImporterFor(serviceName, null, registry, interfaces);
		// define binding listeners
		ManagedList<BeanDefinition> listenerDefinitions = new ManagedList<BeanDefinition>();
		if (element.hasAttribute(CONTROL_BUS)){
			String busBeanName = element.getAttribute(CONTROL_BUS);
			BeanDefinition busBindingListener = 
				ControlBusOSGiConfigUtils.defineBindingMessageDistributor(registry, importerBuilder, busBeanName);
			listenerDefinitions.add(busBindingListener);
		}	
		List<String> dependentSources = ControlBusOSGiConfigUtils.discoverDependentElements(element, serviceName);
		// add binding listener that will execute start/stop on dependent lifecycle beans
		if (dependentSources.size() > 0){
			log.debug("Candidate beans dependent on '" + serviceName + 
					"' and to be managed by IntegrationServiceBindingListener: "  + dependentSources);
			BeanDefinitionBuilder lifecycleBindingManagerBuilder = 
					BeanDefinitionBuilder.genericBeanDefinition(LifecycleServiceManagingListener.class);
			lifecycleBindingManagerBuilder.addPropertyValue("dependentSources", dependentSources);
			listenerDefinitions.add(lifecycleBindingManagerBuilder.getBeanDefinition());
		}
		
		if (listenerDefinitions.size() > 0){
			importerBuilder.addPropertyValue("listeners", listenerDefinitions);
		}
		
		registry.registerBeanDefinition(serviceName, importerBuilder.getBeanDefinition());
	}
	/**
	 */
	private void processRefAttribute(Element element, BeanDefinitionRegistry registry, int configType) {
		if (configType == EXPORT){
			String beanName = element.getAttribute(REF);
			this.generateServiceExorterDefinition(beanName, element, registry);
		} else if (configType == IMPORT){
			
		}
	}
	/**
	 */
	private void processTypeAttribute(Element element, BeanDefinitionRegistry registry, int configType) {
		String exportedTypes = element.getAttribute(TYPE);
		if (configType == EXPORT){
			Element rootDocumentElement = element.getOwnerDocument().getDocumentElement();
			List<Element> elementsToExport = DomUtils.getChildElementsByTagName(rootDocumentElement, exportedTypes);
			for (Element elementToExport : elementsToExport) {
				String beanName = elementToExport.getAttribute("id");
				this.generateServiceExorterDefinition(beanName, element, registry);
			}
		}
	}
	/*
	 * 
	 */
	private void generateServiceExorterDefinition(String beanName, Element element, BeanDefinitionRegistry registry){
		BeanDefinitionBuilder exportedElementBuilder = 
			ControlBusOSGiConfigUtils.defineServiceExporterFor(beanName, registry);
		ManagedList<BeanDefinition> listenerDefinitions = new ManagedList<BeanDefinition>();
		if (element.hasAttribute(CONTROL_BUS)){
			BeanDefinition controlBusMessageDistributorDefinition = 
				this.defineControlBusMessageDistributor(element, exportedElementBuilder, registry, beanName);
			listenerDefinitions.add(controlBusMessageDistributorDefinition);
		}
		// NOTE: add more listeners here if needed
		if (listenerDefinitions.size() > 0){
			exportedElementBuilder.addPropertyValue("listeners", listenerDefinitions);
		}	
		registry.registerBeanDefinition(beanName+EXPORTER_SUFFIX, exportedElementBuilder.getBeanDefinition());
	}
	/**
	 * If element specifies 'control-bus' attribute, this method will register 
	 * {@link ControlBusRegistrationMessageDistributionListener}
	 * which will send registration messages to the ControlBus
	 */
	private BeanDefinition defineControlBusMessageDistributor(Element originalElement, 
												 BeanDefinitionBuilder exportedElementBuilder,
												 BeanDefinitionRegistry registry,
												 String componentName) {
		AbstractBeanDefinition listenerDefinition = null;
		if (originalElement.hasAttribute(CONTROL_BUS)){
			String controlBusAttributeValue = originalElement.getAttribute(CONTROL_BUS);
			Assert.hasText(controlBusAttributeValue, "You must provide control bus name when defining 'control-bus' attribute");
			log.trace("Adding registration listener for exported OSGi service for:" + componentName);
			
			 listenerDefinition = 
				 ControlBusOSGiConfigUtils.defineRegistrationMessageDistributor(registry, exportedElementBuilder, controlBusAttributeValue);
			BeanDefinitionReaderUtils.registerWithGeneratedName(listenerDefinition, registry);
		}
		return listenerDefinition;  
	}
	/**
	 */	
	private String getIncludeType(Element element){
		if (element.hasAttribute(REF)){
			return REF;
		} else if (element.hasAttribute(TYPE)){
			return TYPE;
		} 
		return null;
	}
	/**
	 */
	private Class[] discoverInterfaces(String name, Element element){
		if (element.hasAttribute(TYPE)){
			return SiTypeToJavaTypeMaper.mapSiType(element.getAttribute(TYPE));
		}
		return null;
	}
}
