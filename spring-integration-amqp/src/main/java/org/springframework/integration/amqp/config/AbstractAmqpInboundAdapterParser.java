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

package org.springframework.integration.amqp.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for inbound adapter parsers for the AMQP namespace.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 * @since 2.1
 */
abstract class AbstractAmqpInboundAdapterParser extends AbstractSingleBeanDefinitionParser {

	private static final String[] CONTAINER_VALUE_ATTRIBUTES = {
		"acknowledge-mode",
		"channel-transacted",
		"concurrent-consumers",
		"expose-listener-channel",
		"phase",
		"prefetch-count",
		"queue-names",
		"recovery-interval",
		"receive-timeout",
		"shutdown-timeout",
		"tx-size",
		"missing-queues-fatal"
	};

	private static final String[] CONTAINER_REFERENCE_ATTRIBUTES = {
		"advice-chain",
		"connection-factory",
		"error-handler",
		"message-properties-converter",
		"task-executor",
		"transaction-attribute",
		"transaction-manager"
	};


	private final String adapterClassName;


	AbstractAmqpInboundAdapterParser(String adapterClassName) {
		Assert.hasText(adapterClassName, "adapterClassName is required");
		this.adapterClassName = adapterClassName;
	}


	@Override
	protected final String getBeanClassName(Element element) {
		return this.adapterClassName;
	}

	@Override
	protected final boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected final boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String listenerContainerRef = element.getAttribute("listener-container");
		if (StringUtils.hasText(listenerContainerRef)) {
			assertNoContainerAttributes(element, parserContext);
			builder.addConstructorArgReference(listenerContainerRef);
		}
		else {
			BeanDefinition listenerContainerBeanDef = this.buildListenerContainer(element, parserContext);
			builder.addConstructorArgValue(listenerContainerBeanDef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");

		IntegrationNamespaceUtils.configureHeaderMapper(element, builder, parserContext, DefaultAmqpHeaderMapper.class, null);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "phase");
		this.configureChannels(element, parserContext, builder);
	}

	protected abstract void configureChannels(Element element, ParserContext parserContext, BeanDefinitionBuilder builder);

	private BeanDefinition buildListenerContainer(Element element, ParserContext parserContext) {
		if (!element.hasAttribute("queue-names")) {
			parserContext.getReaderContext().error("If no 'listener-container' reference is provided, " +
					"the 'queue-names' attribute is required.", element);
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer");
		String connectionFactoryRef = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactoryRef)) {
			connectionFactoryRef = "rabbitConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactoryRef);
		for (String attributeName : CONTAINER_VALUE_ATTRIBUTES) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, attributeName);
		}
		for (String attributeName : CONTAINER_REFERENCE_ATTRIBUTES) {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, attributeName);
		}
		return builder.getBeanDefinition();
	}

	private void assertNoContainerAttributes(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		List<String> allContainerAttributes = new ArrayList<String>(Arrays.asList(CONTAINER_VALUE_ATTRIBUTES));
		allContainerAttributes.addAll(Arrays.asList(CONTAINER_REFERENCE_ATTRIBUTES));
		for (String attributeName : allContainerAttributes) {
			if (StringUtils.hasText(element.getAttribute(attributeName))) {
				parserContext.getReaderContext().error("Attribute '" + attributeName
						+ "' is not allowed when a 'listener-container' reference has been provided", source);
			}
		}
	}

}
