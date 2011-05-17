/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class MethodInvokingOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	protected String parseAndRegisterConsumer(Element element, ParserContext parserContext) {
		BeanComponentDefinition consumerDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		String consumerRef = null;
		
		if (consumerDefinition == null){
			consumerRef = element.getAttribute(IntegrationNamespaceUtils.REF_ATTRIBUTE);
		} else {
			consumerRef = consumerDefinition.getBeanName();
		}	
		if (element.hasAttribute(IntegrationNamespaceUtils.METHOD_ATTRIBUTE)) {
			consumerRef = BeanDefinitionReaderUtils.registerWithGeneratedName(
					this.parseConsumer(element, parserContext), parserContext.getRegistry());
		}
		Assert.hasText(consumerRef, "Can not determine consumer for 'outbound-channel-adapter'");
		return consumerRef;
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder invokerBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".handler.MethodInvokingMessageHandler");
		
		invokerBuilder.addPropertyValue("componentType", "outbound-channel-adapter");
		BeanComponentDefinition innerHandlerDefinition = 
					IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		if (innerHandlerDefinition == null){
			Assert.hasText(element.getAttribute(IntegrationNamespaceUtils.REF_ATTRIBUTE), 
							"You must provide 'ref' attribute or register inner bean for " +
							"Outbound Channel consumer.");
			invokerBuilder.addConstructorArgReference(element.getAttribute(IntegrationNamespaceUtils.REF_ATTRIBUTE));
		} else {
			invokerBuilder.addConstructorArgValue(innerHandlerDefinition);
		}
		invokerBuilder.addConstructorArgValue(element.getAttribute(IntegrationNamespaceUtils.METHOD_ATTRIBUTE));
		String order = element.getAttribute(IntegrationNamespaceUtils.ORDER);
		if (StringUtils.hasText(order)) {
			invokerBuilder.addPropertyValue(IntegrationNamespaceUtils.ORDER, order);
		}
		return invokerBuilder.getBeanDefinition();
	}
}
