/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base class for outbound Channel Adapter parsers.
 *
 * If this component is defined as the top-level element in the Spring application context it will produce
 * an {@link org.springframework.integration.endpoint.AbstractEndpoint} depending on the channel type.
 * If this component is defined as nested element (e.g., inside of the chain) it will produce
 * a {@link org.springframework.messaging.MessageHandler}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ngoc Nhan
 */
public abstract class AbstractOutboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		if (parserContext.isNested()) {
			if (channelName != null) {
				String elementDescription = IntegrationNamespaceUtils.createElementDescription(element);
				parserContext.getReaderContext().error(
						"The 'channel' attribute isn't allowed for " +
								elementDescription +
								" when it is used as a nested element," +
								" e.g. inside a <chain/>", element);
			}
			AbstractBeanDefinition consumerBeanDefinition = this.parseConsumer(element, parserContext);
			this.configureRequestHandlerAdviceChain(element, parserContext, consumerBeanDefinition, null);
			return consumerBeanDefinition;
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ConsumerEndpointFactoryBean.class);
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		BeanComponentDefinition handlerBeanComponentDefinition = this.doParseAndRegisterConsumer(element, parserContext);
		builder.addPropertyReference("handler", handlerBeanComponentDefinition.getBeanName());
		IntegrationNamespaceUtils.checkAndConfigureFixedSubscriberChannel(element, parserContext, channelName,
				handlerBeanComponentDefinition.getBeanName());
		if (pollerElement != null) {
			if (!StringUtils.hasText(channelName)) {
				parserContext.getReaderContext().error(
						"outbound channel adapter with a 'poller' requires a 'channel' to poll", element);
			}
			IntegrationNamespaceUtils.configurePollerMetadata(pollerElement, builder, parserContext);
		}
		builder.addPropertyValue("inputChannelName", channelName);

		this.configureRequestHandlerAdviceChain(element, parserContext, handlerBeanComponentDefinition.getBeanDefinition(), builder);

		return builder.getBeanDefinition();
	}

	private void configureRequestHandlerAdviceChain(Element element, ParserContext parserContext,
			BeanDefinition handlerBeanDefinition, @Nullable BeanDefinitionBuilder consumerBuilder) {
		Element txElement = DomUtils.getChildElementByTagName(element, "transactional");
		Element adviceChainElement = DomUtils.getChildElementByTagName(element,
				IntegrationNamespaceUtils.REQUEST_HANDLER_ADVICE_CHAIN);
		@SuppressWarnings("rawtypes")
		ManagedList adviceChain =
				IntegrationNamespaceUtils.configureAdviceChain(adviceChainElement, txElement, handlerBeanDefinition,
						parserContext);
		if (!CollectionUtils.isEmpty(adviceChain)) {
			/*
			 * For ARPMH, the advice chain is injected so just the handleRequestMessage method is advised.
			 * Sometime ARPMHs do double duty as a gateway and a channel adapter. The parser subclass
			 * can indicate this by overriding isUsingReplyProducer(), or we can try to determine it from
			 * the bean class.
			 */
			boolean isReplyProducer = this.isUsingReplyProducer();
			if (!isReplyProducer) {
				Class<?> beanClass = null;
				if (handlerBeanDefinition instanceof AbstractBeanDefinition abstractBeanDefinition) {
					if (abstractBeanDefinition.hasBeanClass()) {
						beanClass = abstractBeanDefinition.getBeanClass();
					}
				}
				isReplyProducer = beanClass != null && AbstractReplyProducingMessageHandler.class.isAssignableFrom(beanClass);
			}

			if (isReplyProducer) {
				handlerBeanDefinition.getPropertyValues().add("adviceChain", adviceChain);
			}
			else if (consumerBuilder != null) {
				consumerBuilder.addPropertyValue("adviceChain", adviceChain);
			}
			else {
				String elementDescription = IntegrationNamespaceUtils.createElementDescription(element);
				parserContext.getReaderContext().error("'request-handler-advice-chain' isn't allowed for " +
						elementDescription +
						" within a <chain/>, because its Handler " +
						"isn't an AbstractReplyProducingMessageHandler", element);
			}
		}
	}

	/**
	 * Override this method to control the registration process and return the bean name.
	 * If parsing a bean definition whose name can be auto-generated, consider using
	 * {@link #parseConsumer(Element, ParserContext)} instead.
	 *
	 * @param element The element.
	 * @param parserContext The parser context.
	 * @return The bean component definition.
	 */
	protected BeanComponentDefinition doParseAndRegisterConsumer(Element element, ParserContext parserContext) {
		AbstractBeanDefinition definition = this.parseConsumer(element, parserContext);
		if (definition == null) {
			parserContext.getReaderContext().error("Consumer parsing must return an AbstractBeanDefinition.", element);
		}
		String order = element.getAttribute(IntegrationNamespaceUtils.ORDER);
		if (StringUtils.hasText(order)) {
			definition.getPropertyValues().addPropertyValue(IntegrationNamespaceUtils.ORDER, order);
		}
		String beanName = BeanDefinitionReaderUtils.generateBeanName(definition, parserContext.getRegistry());
		String[] handlerAlias = IntegrationNamespaceUtils.generateAlias(element);
		BeanComponentDefinition beanComponentDefinition = new BeanComponentDefinition(definition, beanName, handlerAlias);
		parserContext.registerBeanComponent(beanComponentDefinition);
		return beanComponentDefinition;
	}

	/**
	 * Override this method to return the BeanDefinition for the MessageConsumer. It will
	 * be registered with a generated name.
	 *
	 * @param element The element.
	 * @param parserContext The parser context.
	 * @return The bean definition.
	 */
	protected abstract AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext);

	/**
	 * Override this to signal that this channel adapter is actually using a AbstractReplyProducingMessageHandler
	 * while it is not possible for this parser to determine that because, say, a FactoryBean is being used.
	 *
	 * @return false, unless overridden.
	 */
	protected boolean isUsingReplyProducer() {
		return false;
	}

}
