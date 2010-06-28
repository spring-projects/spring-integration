/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.event.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.event.ApplicationEventPublishingMessageHandler;
import org.w3c.dom.Element;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class EventOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser{
	

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element,
			ParserContext parserContext) {
		BeanDefinitionBuilder invokerBuilder = BeanDefinitionBuilder.genericBeanDefinition(ApplicationEventPublishingMessageHandler.class);
//		BeanComponentDefinition innerHandlerDefinition = 
//					IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
//		if (innerHandlerDefinition == null){
//			Assert.hasText(element.getAttribute(IntegrationNamespaceUtils.REF_ATTRIBUTE), 
//							"You must provide 'ref' attribute or register inner bean for " +
//							"Outbound Channel consumer.");
//			invokerBuilder.addConstructorArgReference(element.getAttribute(IntegrationNamespaceUtils.REF_ATTRIBUTE));
//		} else {
//			invokerBuilder.addConstructorArgValue(innerHandlerDefinition);
//		}
//		invokerBuilder.addConstructorArgValue(element.getAttribute(IntegrationNamespaceUtils.METHOD_ATTRIBUTE));
//		String order = element.getAttribute(IntegrationNamespaceUtils.ORDER);
//		if (StringUtils.hasText(order)) {
//			invokerBuilder.addPropertyValue(IntegrationNamespaceUtils.ORDER, order);
//		}
		return invokerBuilder.getBeanDefinition();
	}

	

}
