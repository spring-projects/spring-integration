/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.jms.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;outbound-gateway&gt; element of the integration 'jms' namespace.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class JmsOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JmsOutboundGateway.class);
		builder.addPropertyReference("connectionFactory", element.getAttribute("connection-factory"));
		parseDestination(element, parserContext, builder, "request-destination", "request-destination-name",
				"request-destination-expression", "requestDestination", "requestDestinationName",
				"requestDestinationExpression", true);
		parseDestination(element, parserContext, builder, "reply-destination", "reply-destination-name",
				"reply-destination-expression", "replyDestination", "replyDestinationName",
				"replyDestinationExpression", false);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "correlation-key");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "header-mapper");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "destination-resolver");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-request-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-reply-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "receive-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-pub-sub-domain");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-pub-sub-domain");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "time-to-live");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "priority");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "explicit-qos-enabled");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");

		String deliveryPersistent = element.getAttribute("delivery-persistent");
		if (StringUtils.hasText(deliveryPersistent)) {
			builder.addPropertyValue("deliveryPersistent", deliveryPersistent);
		}
		Element container = DomUtils.getChildElementByTagName(element, "reply-listener");
		
		if (container != null) {
			this.parseReplyContainer(builder, parserContext, container);
		}
		return builder;
	}

	private void parseDestination(Element element, ParserContext parserContext, BeanDefinitionBuilder builder,
			String destinationAttributeName, String destinationNameAttributeName, String destinationExpressionAttributeName,
			String destinationProperty, String destinationNameProperty, String destinationExpressionProperty,
			boolean oneRequired) {
		String destinationAttribute = element.getAttribute(destinationAttributeName);
		String destinationNameAttribute = element.getAttribute(destinationNameAttributeName);
		String destinationExpressionAttribute = element.getAttribute(destinationExpressionAttributeName);
		boolean hasDestination = StringUtils.hasText(destinationAttribute);
		boolean hasDestinationName = StringUtils.hasText(destinationNameAttribute);
		boolean hasDestinationExpression = StringUtils.hasText(destinationExpressionAttribute);
		int destCount = (hasDestination ? 1 : 0) +
						(hasDestinationName ? 1 : 0) +
						(hasDestinationExpression ? 1 : 0);
		if (oneRequired) {
			if (destCount != 1) {
				parserContext.getReaderContext().error("Exactly one of the '" + destinationAttribute + "', " +
						"'" + destinationNameAttributeName + "', or '" + destinationExpressionAttributeName + "' attributes is required.", element);
			}
		}
		else {
			if (destCount > 1) {
				parserContext.getReaderContext().error("Only one of the '" + destinationAttribute + "', " +
						"'" + destinationNameAttributeName + "', or '" + destinationExpressionAttributeName + "' attributes is allowed.", element);
			}
		}
		if (hasDestination) {
			builder.addPropertyReference(destinationProperty, destinationAttribute);
		}
		else if (hasDestinationName) {
			builder.addPropertyValue(destinationNameProperty, destinationNameAttribute);
		}
		else if (hasDestinationExpression) {
			BeanDefinitionBuilder expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
			expressionBuilder.addConstructorArgValue(destinationExpressionAttribute);
			builder.addPropertyValue(destinationExpressionProperty, expressionBuilder.getBeanDefinition());
		}
	}

	private void parseReplyContainer(BeanDefinitionBuilder gatewayBuilder, ParserContext parserContext, Element element) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JmsOutboundGateway.ReplyContainerProperties.class);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "acknowledge", "sessionAcknowledgeModeName");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-messages-per-task");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "receive-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "recovery-interval");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-consumer-limit");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-task-execution-limit");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "cache-level");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");

		gatewayBuilder.addPropertyValue("replyContainerProperties", builder.getBeanDefinition());
	}

}
