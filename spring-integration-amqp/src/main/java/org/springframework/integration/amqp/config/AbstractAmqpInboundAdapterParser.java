/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.integration.amqp.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
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
 * @author Artem Bilan
 *
 * @since 2.1
 */
abstract class AbstractAmqpInboundAdapterParser extends AbstractSingleBeanDefinitionParser {

	private static final String[] CONTAINER_VALUE_ATTRIBUTES = {
			"acknowledge-mode",
			"channel-transacted",
			"concurrent-consumers",
			"consumers-per-queue",
			"expose-listener-channel",
			"phase",
			"prefetch-count",
			"queue-names",
			"recovery-interval",
			"receive-timeout",
			"shutdown-timeout",
			"batch-size",
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
			BeanDefinition listenerContainerBeanDef = buildListenerContainer(element, parserContext);
			builder.addConstructorArgValue(listenerContainerBeanDef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");

		BeanDefinitionBuilder mapperBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultAmqpHeaderMapper.class);
		mapperBuilder.setFactoryMethod("inboundMapper");
		IntegrationNamespaceUtils.configureHeaderMapper(element, builder, parserContext, mapperBuilder, null);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "phase");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "batch-mode");
		configureChannels(element, parserContext, builder);
		AbstractBeanDefinition adapterBeanDefinition = builder.getRawBeanDefinition();
		adapterBeanDefinition.setResource(parserContext.getReaderContext().getResource());
		adapterBeanDefinition.setSource(IntegrationNamespaceUtils.createElementDescription(element));
	}

	private BeanDefinition buildListenerContainer(Element element, ParserContext parserContext) {
		XmlReaderContext readerContext = parserContext.getReaderContext();
		if (!element.hasAttribute("queue-names")) {
			readerContext.error(
					"If no 'listener-container' reference is provided, the 'queue-names' attribute is required.",
					element);
		}
		String consumersPerQueue = element.getAttribute("consumers-per-queue");
		BeanDefinitionBuilder builder;
		if (StringUtils.hasText(consumersPerQueue)) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(DirectMessageListenerContainer.class);
			if (StringUtils.hasText(element.getAttribute("concurrent-consumers"))) {
				readerContext.error("'consumers-per-queue' and 'concurrent-consumers' are mutually exclusive", element);
			}
			if (StringUtils.hasText(element.getAttribute("tx-size"))) {
				readerContext.error("'tx-size' is not allowed with 'consumers-per-queue'", element);
			}
			if (StringUtils.hasText(element.getAttribute("receive-timeout"))) {
				readerContext.error("'receive-timeout' is not allowed with 'consumers-per-queue'", element);
			}
			builder.addPropertyValue("consumersPerQueue", consumersPerQueue);
		}
		else {
			builder = BeanDefinitionBuilder.genericBeanDefinition(SimpleMessageListenerContainer.class);
		}
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
		List<String> allContainerAttributes = new ArrayList<>(Arrays.asList(CONTAINER_VALUE_ATTRIBUTES));
		allContainerAttributes.addAll(Arrays.asList(CONTAINER_REFERENCE_ATTRIBUTES));
		for (String attributeName : allContainerAttributes) {
			if (StringUtils.hasText(element.getAttribute(attributeName))) {
				parserContext.getReaderContext().error("Attribute '" + attributeName
						+ "' is not allowed when a 'listener-container' reference has been provided", source);
			}
		}
	}

	protected abstract void configureChannels(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder);

}
