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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.xml.DomUtils;

/**
 * Base class parser for elements that create Message Endpoints.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractConsumerEndpointParser extends AbstractSingleBeanDefinitionParser {

	protected static final String REF_ATTRIBUTE = "ref";

	protected static final String METHOD_ATTRIBUTE = "method";

	protected static final String OUTPUT_CHANNEL_ATTRIBUTE = "output-channel";

	private static final String POLLER_ELEMENT = "poller";

	private static final String SELECTOR_ATTRIBUTE = "selector";


	@Override
	protected final Class<?> getBeanClass(Element element) {
		return ConsumerEndpointFactoryBean.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	/**
	 * Parse the MessageConsumer.
	 */
	protected abstract BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext);

	protected String getInputChannelAttributeName() {
		return "input-channel";
	}

	@Override
	protected final void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		BeanDefinitionBuilder consumerBuilder = this.parseHandler(element, parserContext);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(consumerBuilder, element, OUTPUT_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(consumerBuilder, element, SELECTOR_ATTRIBUTE);
		String consumerBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				consumerBuilder.getBeanDefinition(), parserContext.getRegistry());
		builder.addConstructorArgReference(consumerBeanName);
		String inputChannelAttributeName = this.getInputChannelAttributeName();
		String inputChannelName = element.getAttribute(inputChannelAttributeName);
		Assert.hasText(inputChannelName, "the '" + inputChannelAttributeName + "' attribute is required");
		if (!parserContext.getRegistry().containsBeanDefinition(inputChannelName)) {
			BeanDefinitionBuilder channelDef = BeanDefinitionBuilder.genericBeanDefinition(DirectChannel.class);
			BeanDefinitionHolder holder = new BeanDefinitionHolder(channelDef.getBeanDefinition(), inputChannelName);
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		}
		builder.addPropertyValue("inputChannelName", inputChannelName);
		Element pollerElement = DomUtils.getChildElementByTagName(element, POLLER_ELEMENT);
		if (pollerElement != null) {
			IntegrationNamespaceUtils.configureTrigger(pollerElement, builder, parserContext);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, pollerElement, "max-messages-per-poll");
			Element txElement = DomUtils.getChildElementByTagName(pollerElement, "transactional");
			if (txElement != null) {
				IntegrationNamespaceUtils.configureTransactionAttributes(txElement, builder);
			}
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, pollerElement, "task-executor");
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		this.postProcess(element, parserContext, builder);
	}

	/**
	 * Subclasses may implement this method to provide additional configuration.
	 */
	protected void postProcess(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
	}

}
