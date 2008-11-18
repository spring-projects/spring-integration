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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.xml.DomUtils;

/**
 * Base class for outbound Channel Adapter parsers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractOutboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		BeanDefinitionBuilder builder =  BeanDefinitionBuilder.genericBeanDefinition(ConsumerEndpointFactoryBean.class);
		builder.addConstructorArgReference(this.parseAndRegisterConsumer(element, parserContext));
		if (pollerElement != null) {
			Assert.hasText(channelName, "outbound channel adapter with a 'poller' requires a 'channel' to poll");
			IntegrationNamespaceUtils.configureTrigger(pollerElement, builder, parserContext);
			Element txElement = DomUtils.getChildElementByTagName(pollerElement, "transactional");
			if (txElement != null) {
				IntegrationNamespaceUtils.configureTransactionAttributes(txElement, builder);
			}
		}
		builder.addPropertyValue("inputChannelName", channelName);
		return builder.getBeanDefinition();
	}

	/**
	 * Override this method to control the registration process and return the bean name.
	 * If parsing a bean definition whose name can be auto-generated, consider using
	 * {@link #parseConsumer(Element, ParserContext)} instead.
	 */
	protected String parseAndRegisterConsumer(Element element, ParserContext parserContext) {
		AbstractBeanDefinition definition = this.parseConsumer(element, parserContext);
		Assert.notNull(definition, "Consumer parsing must return a BeanDefinition.");
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				definition, parserContext.getRegistry());
	}

	/**
	 * Override this method to return the BeanDefinition for the MessageConsumer. It will
	 * be registered with a generated name.
	 */
	protected abstract AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext);

}
