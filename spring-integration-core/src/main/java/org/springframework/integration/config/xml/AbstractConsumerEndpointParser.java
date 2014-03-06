/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base class parser for elements that create Message Endpoints.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractConsumerEndpointParser extends AbstractBeanDefinitionParser {

	protected static final String REF_ATTRIBUTE = "ref";

	protected static final String METHOD_ATTRIBUTE = "method";

	protected static final String EXPRESSION_ATTRIBUTE = "expression";

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String id = element.getAttribute(ID_ATTRIBUTE);
		if (!StringUtils.hasText(id)) {
			id = element.getAttribute("name");
		}
		if (!StringUtils.hasText(id)) {
			id = BeanDefinitionReaderUtils.generateBeanName(definition, parserContext.getRegistry(), parserContext.isNested());
		}
		return id;
	}

	/**
	 * Parse the MessageHandler.
	 *
	 * @param element The element.
	 * @param parserContext The parser context.
	 * @return The bean definition builder.
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

		Element adviceChainElement = DomUtils.getChildElementByTagName(element,
				IntegrationNamespaceUtils.REQUEST_HANDLER_ADVICE_CHAIN);
		IntegrationNamespaceUtils.configureAndSetAdviceChainIfPresent(adviceChainElement, null,
				handlerBuilder.getRawBeanDefinition(), parserContext);

		AbstractBeanDefinition handlerBeanDefinition = handlerBuilder.getBeanDefinition();
		String inputChannelAttributeName = this.getInputChannelAttributeName();
		boolean hasInputChannelAttribute = element.hasAttribute(inputChannelAttributeName);
		if (parserContext.isNested()) {
			if (hasInputChannelAttribute) {
				String elementDescription = IntegrationNamespaceUtils.createElementDescription(element);
				parserContext.getReaderContext().error("The '" + inputChannelAttributeName
						+ "' attribute isn't allowed for a nested (e.g. inside a <chain/>) endpoint element: "
						+ elementDescription + ".", element);
			}
			return handlerBeanDefinition;
		}
		else {
			if (!hasInputChannelAttribute) {
				String elementDescription = IntegrationNamespaceUtils.createElementDescription(element);
				parserContext.getReaderContext().error("The '" + inputChannelAttributeName
						+ "' attribute is required for the top-level endpoint element: "
						+ elementDescription + ".", element);
			}
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ConsumerEndpointFactoryBean.class);

		String handlerBeanName = BeanDefinitionReaderUtils.generateBeanName(handlerBeanDefinition, parserContext.getRegistry());
		String[] handlerAlias = IntegrationNamespaceUtils.generateAlias(element);
		parserContext.registerBeanComponent(new BeanComponentDefinition(handlerBeanDefinition, handlerBeanName, handlerAlias));

		builder.addPropertyReference("handler", handlerBeanName);

		String inputChannelName = element.getAttribute(inputChannelAttributeName);

		if (!parserContext.getRegistry().containsBeanDefinition(inputChannelName)) {
			if (parserContext.getRegistry().containsBeanDefinition(IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME)) {
				BeanDefinition channelRegistry = parserContext.getRegistry().
						getBeanDefinition(IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
				ConstructorArgumentValues caValues = channelRegistry.getConstructorArgumentValues();
				ValueHolder vh = caValues.getArgumentValue(0, Collection.class);
				if (vh == null) { //although it should never happen if it does we can fix it
					caValues.addIndexedArgumentValue(0, new ManagedSet<String>());
				}

				@SuppressWarnings("unchecked")
				Collection<String> channelCandidateNames = (Collection<String>) caValues.getArgumentValue(0, Collection.class).getValue();
				channelCandidateNames.add(inputChannelName);
			}
			else {
				parserContext.getReaderContext().error("Failed to locate '" +
						IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME + "'", parserContext.getRegistry());
			}
		}
		IntegrationNamespaceUtils.checkAndConfigureFixedSubscriberChannel(element, parserContext, inputChannelName,
				handlerBeanName);

		builder.addPropertyValue("inputChannelName", inputChannelName);
		List<Element> pollerElementList = DomUtils.getChildElementsByTagName(element, "poller");
		if (!CollectionUtils.isEmpty(pollerElementList)) {
			if (pollerElementList.size() != 1) {
				parserContext.getReaderContext().error(
						"at most one poller element may be configured for an endpoint", element);
			}
			IntegrationNamespaceUtils.configurePollerMetadata(pollerElementList.get(0), builder, parserContext);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.AUTO_STARTUP);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.PHASE);
		AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		String beanName = this.resolveId(element, beanDefinition, parserContext);
		parserContext.registerBeanComponent(new BeanComponentDefinition(beanDefinition, beanName));
		return null;
	}

}
