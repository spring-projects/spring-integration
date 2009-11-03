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

import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.intergration.osgi.OSGiIntegrationControlBus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
		return beanName;
	}
	/**
	 * 
	 */
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String busGroupName = element.getAttribute("group-name");
		Assert.isTrue(StringUtils.hasText(busGroupName), "bus-config 'group-name' attribute must be provided");
		beanName = "controlBus-" + busGroupName;
		if (parserContext.getRegistry().containsBeanDefinition(beanName)){
			throw new BeanDefinitionStoreException("You atempted to register a second instance of the Control Bus with the same 'group-name' " +
					"in the single Application Context which is not allowed.");
		}
		
		BeanDefinitionBuilder rootBuilder = BeanDefinitionBuilder.rootBeanDefinition(OSGiIntegrationControlBus.class);
		
		//String busChannelName = "controlMessagesDistributionChannel";
			//this.registerPubSubChannelDefinition(element.getAttribute("task-executor"), element, parserContext);
		rootBuilder.addConstructorArgReference("controlMessagesDistributionChannel");
		
		AbstractOSGiServiceManagingParserUtil.registerServiceExporterFor(beanName, parserContext.getRegistry());
		return rootBuilder.getBeanDefinition();
	}
	/**
	 * 
	 * @param taskExecutorName
	 * @param element
	 * @param parserContext
	 * @return
	 */
	private String registerPubSubChannelDefinition(String taskExecutorName, Element element, ParserContext parserContext){
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(PublishSubscribeChannel.class.getName());
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-handler");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-failures");
		if (!StringUtils.hasText(taskExecutorName)) {
			taskExecutorName = this.createTaskExecutorDefinition(element, parserContext);
		}
		builder.addConstructorArgReference(taskExecutorName);
		
		String beanName = BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
		return beanName;
	}
	/**
	 * 
	 * @param element
	 * @param parserContext
	 * @return
	 */
	private String createTaskExecutorDefinition(Element element, ParserContext parserContext){
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskExecutor.class.getName());
		builder.addPropertyValue("corePoolSize", 5);
		builder.addPropertyValue("maxPoolSize", 10);
		builder.addPropertyValue("rejectedExecutionHandler", new ThreadPoolExecutor.DiscardPolicy());
		String beanName = BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
		return beanName;
	}
	
}
