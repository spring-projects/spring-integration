/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jms.config;

import javax.jms.Session;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'channel' and 'publish-subscribe-channel' elements of the
 * Spring Integration JMS namespace.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakusky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class JmsChannelParser extends AbstractChannelParser {

	private static final String CONTAINER_TYPE_ATTRIBUTE = "container-type";

	private static final String CONTAINER_CLASS_ATTRIBUTE = "container-class";

	private static final String ACKNOWLEDGE_ATTRIBUTE = "acknowledge";


	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(JmsChannelFactoryBean.class);
		String messageDriven = element.getAttribute("message-driven");
		if (StringUtils.hasText(messageDriven)) {
			builder.addConstructorArgValue(messageDriven);
		}
		builder.addPropertyReference(JmsParserUtils.CONNECTION_FACTORY_PROPERTY,
				JmsParserUtils.determineConnectionFactoryBeanName(element, parserContext));
		if ("channel".equals(element.getLocalName())) {
			parseDestination(element, parserContext, builder, "queue");
		}
		else if ("publish-subscribe-channel".equals(element.getLocalName())) {
			parseDestination(element, parserContext, builder, "topic");
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-subscribers");

		String containerType = element.getAttribute(CONTAINER_TYPE_ATTRIBUTE);
		setContainerClass(element, builder, containerType);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "receive-timeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "transaction-manager");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-handler");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "selector", "messageSelector");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "phase");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delivery-persistent");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "time-to-live");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "priority");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "explicit-qos-enabled");
		setCache(element, parserContext, builder, containerType);
		setAcknowledgeMode(element, parserContext, builder, containerType);
		return builder;
	}

	private static void parseDestination(Element element, ParserContext parserContext, BeanDefinitionBuilder builder,
			String type) {

		boolean isPubSub = "topic".equals(type);
		String ref = element.getAttribute(type);
		String name = element.getAttribute(type + "-name");
		boolean isReference = StringUtils.hasText(ref);
		boolean isName = StringUtils.hasText(name);
		if (isReference == isName) {
			parserContext.getReaderContext().error("Exactly one of the '" + type +
					"' or '" + type + "-name' attributes is required.", element);
		}
		if (isReference) {
			builder.addPropertyReference("destination", ref);
		}
		else if (isName) {
			builder.addPropertyValue("destinationName", name);
			builder.addPropertyValue("pubSubDomain", isPubSub);
			String destinationResolver = element.getAttribute("destination-resolver");
			if (StringUtils.hasText(destinationResolver)) {
				builder.addPropertyReference("destinationResolver", destinationResolver);
			}
		}
		if (isPubSub) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "durable", "subscriptionDurable");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "subscription",
					"durableSubscriptionName");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "subscription-shared");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "client-id");
		}
	}

	private static void setContainerClass(Element element, BeanDefinitionBuilder builder, String containerType) {
		String containerClass = element.getAttribute(CONTAINER_CLASS_ATTRIBUTE);
		if (!StringUtils.hasText(containerClass) && StringUtils.hasText(containerType)) {
			if ("default".equals(containerType)) {
				containerClass = "org.springframework.jms.listener.DefaultMessageListenerContainer";
			}
			else if ("simple".equals(containerType)) {
				containerClass = "org.springframework.jms.listener.SimpleMessageListenerContainer";
			}
		}
		/*
		 * Schema docs tell the user to ensure that, if they supply a container-class, it
		 * is their responsibility to set the container-type appropriately, based on the superclass
		 * of their implementation (default="default"). If it is set incorrectly, some attributes
		 * may not be applied.
		 * We cannot reliably infer it here.
		 */
		if (StringUtils.hasText(containerClass)) {
			builder.addPropertyValue("containerType", containerClass);
		}
	}

	private static void setCache(Element element, ParserContext parserContext, BeanDefinitionBuilder builder,
			String containerType) {

		String cache = element.getAttribute("cache");
		if (StringUtils.hasText(cache)) {
			if (containerType.startsWith("simple")) {
				if (!("auto".equals(cache) || "consumer".equals(cache))) {
					parserContext.getReaderContext().warning(
							"'cache' attribute not actively supported for listener container of type \"simple\". " +
									"Effective runtime behavior will be equivalent to \"consumer\" / \"auto\".",
							element);
				}
			}
			else {
				builder.addPropertyValue("cacheLevelName", "CACHE_" + cache.toUpperCase());
			}
		}
	}

	private static void setAcknowledgeMode(Element element, ParserContext parserContext, BeanDefinitionBuilder builder,
			String containerType) {

		Integer acknowledgeMode = parseAcknowledgeMode(element, parserContext);
		if (acknowledgeMode != null) {
			if (acknowledgeMode == Session.SESSION_TRANSACTED) {
				builder.addPropertyValue("sessionTransacted", Boolean.TRUE);
			}
			else {
				builder.addPropertyValue("sessionAcknowledgeMode", acknowledgeMode);
			}
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "concurrency");

		String prefetch = element.getAttribute("prefetch");
		if (StringUtils.hasText(prefetch) && containerType.startsWith("default")) {
			builder.addPropertyValue("maxMessagesPerTask", Integer.valueOf(prefetch));
		}
	}

	private static Integer parseAcknowledgeMode(Element ele, ParserContext parserContext) {
		String acknowledge = ele.getAttribute(ACKNOWLEDGE_ATTRIBUTE);
		if (StringUtils.hasText(acknowledge)) {
			int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
			if ("transacted".equals(acknowledge)) {
				acknowledgeMode = Session.SESSION_TRANSACTED;
			}
			else if ("dups-ok".equals(acknowledge)) {
				acknowledgeMode = Session.DUPS_OK_ACKNOWLEDGE;
			}
			else if ("client".equals(acknowledge)) {
				acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;
			}
			else if (!"auto".equals(acknowledge)) {
				parserContext.getReaderContext().error("Invalid JMS Channel 'acknowledge' setting [" +
						acknowledge + "]: only \"auto\", \"client\", \"dups-ok\" and \"transacted\" supported.", ele);
			}
			return acknowledgeMode;
		}
		else {
			return null;
		}
	}

}
