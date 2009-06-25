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

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base class parser for elements that create Message Endpoints.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public abstract class AbstractConsumerEndpointParser extends AbstractBeanDefinitionParser {

	protected static final String REF_ATTRIBUTE = "ref";

	protected static final String METHOD_ATTRIBUTE = "method";


	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	/**
	 * Parse the MessageHandler.
	 */
	protected abstract BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext);

	protected String getInputChannelAttributeName() {
		return "input-channel";
	}

	@Override
	protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = this.parseHandler(element, parserContext);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(handlerBuilder, element, "output-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(handlerBuilder, element, "order");
		AbstractBeanDefinition handlerBeanDefinition = handlerBuilder.getBeanDefinition();
		String inputChannelAttributeName = this.getInputChannelAttributeName();
		if (!element.hasAttribute(inputChannelAttributeName)) {
			if (!parserContext.isNested()) {
				String elementDescription = IntegrationNamespaceUtils.createElementDescription(element);
				parserContext.getReaderContext().error("The '" + inputChannelAttributeName
						+ "' attribute is required for the top-level endpoint element "
						+ elementDescription + ".", element);
			}
			return handlerBeanDefinition;
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".config.ConsumerEndpointFactoryBean");
		String handlerBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(handlerBeanDefinition, parserContext.getRegistry());
		builder.addPropertyReference("handler", handlerBeanName);
		String inputChannelName = element.getAttribute(inputChannelAttributeName);
		boolean channelExists = false;
		if (parserContext.getRegistry() instanceof BeanFactory) {
			// BeanFactory also checks ancestor contexts in a hierarchy
			channelExists = ((BeanFactory) parserContext.getRegistry()).containsBean(inputChannelName);
		}
		else {
			channelExists = parserContext.getRegistry().containsBeanDefinition(inputChannelName);
		}
		if (!channelExists) {
			// create a default DirectChannel instance
			BeanDefinitionBuilder channelDef = BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".channel.DirectChannel");
			BeanDefinitionHolder holder = new BeanDefinitionHolder(channelDef.getBeanDefinition(), inputChannelName);
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		}
		builder.addPropertyValue("inputChannelName", inputChannelName);
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		if (pollerElement != null) {
			IntegrationNamespaceUtils.configurePollerMetadata(pollerElement, builder, parserContext);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		return builder.getBeanDefinition();
	}

	@SuppressWarnings("unchecked")
	protected BeanDefinition parseInnerHandlerDefinition(Element element, ParserContext parserContext){
		// parses out inner bean definition for concrete implementation if defined
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "bean");
		BeanDefinition innerDefinition = null;
		if (childElements != null && childElements.size() == 1){
			Element beanElement = childElements.get(0);
			BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
			innerDefinition = delegate.parseBeanDefinitionElement(beanElement).getBeanDefinition();
		}
		
		String ref = element.getAttribute(REF_ATTRIBUTE);
		Assert.isTrue(!(StringUtils.hasText(ref) && innerDefinition != null), "Ambiguous definition. Inner bean " + 
				(innerDefinition == null ? innerDefinition : innerDefinition.getBeanClassName()) + " declaration and \"ref\" " + ref + 
		       " are not allowed together.");
		return innerDefinition;
	}

}
