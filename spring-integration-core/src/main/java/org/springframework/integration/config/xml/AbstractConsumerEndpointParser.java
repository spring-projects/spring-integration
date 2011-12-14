/*
 * Copyright 2002-2011 the original author or authors.
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
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.CollectionUtils;
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

	protected static final String EXPRESSION_ATTRIBUTE = "expression";


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

		String handlerBeanName = BeanDefinitionReaderUtils.generateBeanName(handlerBeanDefinition, parserContext.getRegistry());
		parserContext.registerBeanComponent(new BeanComponentDefinition(handlerBeanDefinition, handlerBeanName));
		
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
		if (!channelExists && this.shouldAutoCreateChannel(inputChannelName)) {
			BeanDefinitionBuilder channelDef = BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".channel.DirectChannel");
			BeanDefinitionHolder holder = new BeanDefinitionHolder(channelDef.getBeanDefinition(), inputChannelName);
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		}
		builder.addPropertyValue("inputChannelName", inputChannelName);
		List<Element> pollerElementList = DomUtils.getChildElementsByTagName(element, "poller");
		if (!CollectionUtils.isEmpty(pollerElementList)) {
			if (pollerElementList.size() != 1) {
				parserContext.getReaderContext().error(
						"at most one poller element may be configured for an endpoint", element);
			}
			IntegrationNamespaceUtils.configurePollerMetadata(pollerElementList.get(0), builder, parserContext);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		String beanName = this.resolveId(element, beanDefinition, parserContext);
		parserContext.registerBeanComponent(new BeanComponentDefinition(beanDefinition, beanName));
		return null;
	}

	private boolean shouldAutoCreateChannel(String channelName) {
		return !IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME.equals(channelName)
				&& !IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME.equals(channelName);
	}

}
