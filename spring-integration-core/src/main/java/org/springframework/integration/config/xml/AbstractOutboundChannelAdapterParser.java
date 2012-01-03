/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base class for outbound Channel Adapter parsers.
 *
 * If this component is defined as the top-level element in the Spring application context it will produce
 * an {@link org.springframework.integration.endpoint.AbstractEndpoint} depending on the channel type.
 * If this component is defined as nested element (e.g., inside of the chain) it will produce
 * a {@link org.springframework.integration.core.MessageHandler}.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractOutboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		if (parserContext.isNested()) {
			return this.parseConsumer(element, parserContext);
		}
		BeanDefinitionBuilder builder =  BeanDefinitionBuilder.genericBeanDefinition(ConsumerEndpointFactoryBean.class);
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		builder.addPropertyReference("handler", this.parseAndRegisterConsumer(element, parserContext));
		if (pollerElement != null) {
			if (!StringUtils.hasText(channelName)) {
				parserContext.getReaderContext().error(
						"outbound channel adapter with a 'poller' requires a 'channel' to poll", element);
			}
			IntegrationNamespaceUtils.configurePollerMetadata(pollerElement, builder, parserContext);
		}
		builder.addPropertyValue("inputChannelName", channelName);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		return builder.getBeanDefinition();
	}

	/**
	 * Override this method to control the registration process and return the bean name.
	 * If parsing a bean definition whose name can be auto-generated, consider using
	 * {@link #parseConsumer(Element, ParserContext)} instead.
	 */
	protected String parseAndRegisterConsumer(Element element, ParserContext parserContext) {
		AbstractBeanDefinition definition = this.parseConsumer(element, parserContext);
		if (definition == null) {
			parserContext.getReaderContext().error("Consumer parsing must return an AbstractBeanDefinition.", element);
		}
		String order = element.getAttribute(IntegrationNamespaceUtils.ORDER);
		if (StringUtils.hasText(order)) {
			definition.getPropertyValues().addPropertyValue(IntegrationNamespaceUtils.ORDER, order);
		}
		String beanName = BeanDefinitionReaderUtils.generateBeanName(definition, parserContext.getRegistry());
		parserContext.registerBeanComponent(new BeanComponentDefinition(definition, beanName));
		return beanName;
	}

	/**
	 * Override this method to return the BeanDefinition for the MessageConsumer. It will
	 * be registered with a generated name.
	 */
	protected abstract AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext);

}
