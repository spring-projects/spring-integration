/*
 * Copyright 2002-2020 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;message-driven-channel-adapter&gt; element and the
 * &lt;inbound-gateway&gt; element of the 'jms' namespace.
 *
 * @author Mark Fisher
 * @author Michael Bannister
 * @author Gary Russell
 * @author Artem Bilan
 */
public class JmsMessageDrivenEndpointParser extends AbstractSingleBeanDefinitionParser {

	private static final String DEFAULT_REPLY_DESTINATION_ATTRIB = "default-reply-destination";

	private static final String DEFAULT_REPLY_QUEUE_NAME_ATTRIB = "default-reply-queue-name";

	private static final String DEFAULT_REPLY_TOPIC_NAME_ATTRIB = "default-reply-topic-name";

	private static final String REPLY_TIME_TO_LIVE = "reply-time-to-live";

	private static final String REPLY_PRIORITY = "reply-priority";

	private static final String REPLY_DELIVERY_PERSISTENT = "reply-delivery-persistent";

	private static final String EXPLICIT_QOS_ENABLED_FOR_REPLIES = "explicit-qos-enabled-for-replies";


	private static final String[] CONTAINER_ATTRIBUTES =
			{
					JmsParserUtils.CONNECTION_FACTORY_PROPERTY,
					JmsParserUtils.DESTINATION_ATTRIBUTE,
					JmsParserUtils.DESTINATION_NAME_ATTRIBUTE,
					"destination-resolver", "transaction-manager",
					"concurrent-consumers", "max-concurrent-consumers",
					"acknowledge",
					"max-messages-per-task", "selector",
					"receive-timeout", "recovery-interval",
					"idle-consumer-limit", "idle-task-execution-limit",
					"cache-level", "subscription-durable",
					"subscription-shared", "subscription-name",
					"client-id", "task-executor"
			};


	private final boolean expectReply;


	public JmsMessageDrivenEndpointParser(boolean expectReply) {
		this.expectReply = expectReply;
	}


	@Override
	protected String getBeanClassName(Element element) {
		return JmsMessageDrivenEndpoint.class.getName();
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		String id = super.resolveId(element, definition, parserContext);

		if (!this.expectReply && !element.hasAttribute("channel")) {
			// the created channel will get the 'id', so the adapter's bean name includes a suffix
			id = id + ".adapter";
		}
		if (!StringUtils.hasText(id)) {
			id = BeanDefinitionReaderUtils.generateBeanName(definition, parserContext.getRegistry());
		}

		return id;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String containerBeanName = parseMessageListenerContainer(element, parserContext, builder.getRawBeanDefinition());
		String listenerBeanName = parseMessageListener(element, parserContext, builder.getRawBeanDefinition());
		builder.addConstructorArgReference(containerBeanName);
		builder.addConstructorArgReference(listenerBeanName);
		builder.addConstructorArgValue(hasExternalContainer(element));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.AUTO_STARTUP);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.PHASE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.ROLE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "acknowledge", "sessionAcknowledgeMode");

	}

	private String parseMessageListenerContainer(Element element, ParserContext parserContext,
			BeanDefinition adapterBeanDefinition) {

		String containerClass = element.getAttribute("container-class");
		if (hasExternalContainer(element)) {
			return parseExternalContainer(element, parserContext, containerClass);
		}
		// otherwise, we build a DefaultMessageListenerContainer instance
		BeanDefinitionBuilder builder;
		if (StringUtils.hasText(containerClass)) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(containerClass);
		}
		else {
			builder = BeanDefinitionBuilder.genericBeanDefinition(DefaultMessageListenerContainer.class);
		}
		String destinationAttribute = this.expectReply ? "request-destination" : "destination";
		String destinationNameAttribute = this.expectReply ? "request-destination-name" : "destination-name";
		String pubSubDomainAttribute = this.expectReply ? "request-pub-sub-domain" : "pub-sub-domain";
		String destination = element.getAttribute(destinationAttribute);
		String destinationName = element.getAttribute(destinationNameAttribute);
		boolean hasDestination = StringUtils.hasText(destination);
		boolean hasDestinationName = StringUtils.hasText(destinationName);
		if (hasDestination == hasDestinationName) {
			parserContext.getReaderContext()
					.error("Exactly one of '" + destinationAttribute +
							"' or '" + destinationNameAttribute + "' is required.", element);
		}
		builder.addPropertyReference(JmsParserUtils.CONNECTION_FACTORY_PROPERTY,
				JmsParserUtils.determineConnectionFactoryBeanName(element, parserContext));
		if (hasDestination) {
			builder.addPropertyReference("destination", destination);
		}
		else {
			builder.addPropertyValue("destinationName", destinationName);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, pubSubDomainAttribute,
					"pubSubDomain");
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "destination-resolver");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "transaction-manager");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "selector", "messageSelector");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-messages-per-task");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "receive-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "recovery-interval");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-consumer-limit");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-task-execution-limit");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "cache-level");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "subscription-durable");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "subscription-shared");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "subscription-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "client-id");
		String beanName = adapterBeanNameRoot(element, parserContext, adapterBeanDefinition)
				+ ".container";
		parserContext.getRegistry().registerBeanDefinition(beanName, builder.getBeanDefinition());
		return beanName;
	}

	private String parseExternalContainer(Element element, ParserContext parserContext, String containerClass) {
		if (StringUtils.hasText(containerClass)) {
			parserContext.getReaderContext().error("Cannot have both 'container' and 'container-class'", element);
		}
		for (String containerAttribute : CONTAINER_ATTRIBUTES) {
			if (element.hasAttribute(containerAttribute)) {
				parserContext.getReaderContext().error("The '" + containerAttribute +
						"' attribute should not be provided when specifying a 'container' reference.", element);
			}
		}
		return element.getAttribute("container");
	}


	private boolean hasExternalContainer(Element element) {
		return element.hasAttribute("container");
	}

	private String parseMessageListener(Element element, ParserContext parserContext,
			BeanDefinition adapterBeanDefinition) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(ChannelPublishingJmsMessageListener.class);
		builder.addPropertyValue("expectReply", this.expectReply);
		if (this.expectReply) {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-request-payload");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-reply-payload");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "correlation-key");
			int defaults = 0;
			if (StringUtils.hasText(element.getAttribute(DEFAULT_REPLY_DESTINATION_ATTRIB))) {
				defaults++;
			}
			if (StringUtils.hasText(element.getAttribute(DEFAULT_REPLY_QUEUE_NAME_ATTRIB))) {
				defaults++;
			}
			if (StringUtils.hasText(element.getAttribute(DEFAULT_REPLY_TOPIC_NAME_ATTRIB))) {
				defaults++;
			}
			if (defaults > 1) {
				parserContext.getReaderContext().error("At most one of '" + DEFAULT_REPLY_DESTINATION_ATTRIB
						+ "', '" + DEFAULT_REPLY_QUEUE_NAME_ATTRIB + "', or '" + DEFAULT_REPLY_TOPIC_NAME_ATTRIB
						+ "' may be provided.", element);
			}
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, DEFAULT_REPLY_DESTINATION_ATTRIB);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, DEFAULT_REPLY_QUEUE_NAME_ATTRIB);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, DEFAULT_REPLY_TOPIC_NAME_ATTRIB);
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "destination-resolver");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, REPLY_TIME_TO_LIVE);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, REPLY_PRIORITY);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, REPLY_DELIVERY_PERSISTENT);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, EXPLICIT_QOS_ENABLED_FOR_REPLIES);
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		}
		else {
			String channelName = element.getAttribute("channel");
			if (!StringUtils.hasText(channelName)) {
				channelName = IntegrationNamespaceUtils.createDirectChannel(element, parserContext);
			}
			builder.addPropertyReference("requestChannel", channelName);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout", "requestTimeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload",
					"extractRequestPayload");
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "header-mapper");
		String alias = adapterBeanNameRoot(element, parserContext, adapterBeanDefinition)
				+ ".listener";
		BeanDefinition beanDefinition = builder.getBeanDefinition();
		String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, parserContext.getRegistry());
		BeanComponentDefinition component = new BeanComponentDefinition(beanDefinition, beanName, new String[]{ alias });
		parserContext.registerBeanComponent(component);
		return beanName;
	}

	private String adapterBeanNameRoot(Element element, ParserContext parserContext,
			BeanDefinition adapterBeanDefinition) {
		String beanName = element.getAttribute(ID_ATTRIBUTE);
		if (!StringUtils.hasText(beanName)) {
			beanName = BeanDefinitionReaderUtils.generateBeanName(adapterBeanDefinition, parserContext.getRegistry());
		}
		return beanName;
	}

}
