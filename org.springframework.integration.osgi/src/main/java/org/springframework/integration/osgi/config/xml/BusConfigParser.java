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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.controlbus.ControlBus;
import org.springframework.integration.osgi.OSGiIntegrationControlBus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser to handle 'bus-config' element.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class BusConfigParser extends AbstractBeanDefinitionParser {
	private static final Log log = LogFactory.getLog(BusConfigParser.class);
	
	private String beanName;
	/**
	 * 
	 */
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		return  beanName;
	}
	/**
	 * 
	 */
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String busGroupName = element.getAttribute("group-name");
		Assert.isTrue(StringUtils.hasText(busGroupName), "bus-config 'group-name' attribute must be provided");
		beanName = busGroupName;
		if (parserContext.getRegistry().containsBeanDefinition(beanName)){
			throw new BeanDefinitionStoreException("You atempted to register a second instance of the Control Bus with the same 'group-name' " +
					"in the single Application Context which is not allowed.");
		}
		
		BeanDefinitionBuilder rootBuilder = BeanDefinitionBuilder.rootBeanDefinition(OSGiIntegrationControlBus.class);
		rootBuilder.addConstructorArgReference(ControlBusOSGiConfigUtils.DEFAULT_CONTROL_DIST_CHANNEL);
		rootBuilder.addConstructorArgValue(beanName);
		
		BeanDefinitionBuilder osgiServiceDefinition = 
			ControlBusOSGiConfigUtils.defineServiceExporterFor(beanName, parserContext.getRegistry(), ControlBus.class);
		BeanDefinitionReaderUtils.registerWithGeneratedName(osgiServiceDefinition.getBeanDefinition(), parserContext.getRegistry());
		
		// NOTE add listeners
		log.trace("Control Bus " + beanName + " was parsed successfully");
		return rootBuilder.getBeanDefinition();
	}
}
