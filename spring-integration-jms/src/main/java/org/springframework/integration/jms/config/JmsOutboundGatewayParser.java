/*
 * Copyright 2002-2011 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-gateway&gt; element of the integration 'jms' namespace.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class JmsOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.jms.JmsOutboundGateway");
		builder.addPropertyReference("connectionFactory", element.getAttribute("connection-factory"));
		String requestDestination = element.getAttribute("request-destination");
		String requestDestinationName = element.getAttribute("request-destination-name");
		String requestDestinationExpression = element.getAttribute("request-destination-expression");
		boolean hasRequestDestination = StringUtils.hasText(requestDestination);
		boolean hasRequestDestinationName = StringUtils.hasText(requestDestinationName);
		boolean hasRequestDestinationExpression = StringUtils.hasText(requestDestinationExpression);
		if (!(hasRequestDestination ^ hasRequestDestinationName ^ hasRequestDestinationExpression)) {
			parserContext.getReaderContext().error("Exactly one of the 'request-destination', " +
					"'request-destination-name', or 'request-destination-expression' attributes is required.", element);
		}
		if (hasRequestDestination) {
			builder.addPropertyReference("requestDestination", requestDestination);
		}
		else if (hasRequestDestinationName) {
			builder.addPropertyValue("requestDestinationName", requestDestinationName);
		}
		else if (hasRequestDestinationExpression) {
			BeanDefinitionBuilder expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
			expressionBuilder.addConstructorArgValue(requestDestinationExpression);
			builder.addPropertyValue("requestDestinationExpression", expressionBuilder.getBeanDefinition());
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-destination");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-destination-name");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "correlation-key");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "header-mapper");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "destination-resolver");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-request-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-reply-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "receive-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-pub-sub-domain");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-pub-sub-domain");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "time-to-live");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "priority");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "explicit-qos-enabled");

		String deliveryMode = element.getAttribute("delivery-mode");
		String deliveryPersistent = element.getAttribute("delivery-persistent");
		if (StringUtils.hasText(deliveryMode) && StringUtils.hasText(deliveryPersistent)) {
			parserContext.getReaderContext().error(
					"The 'delivery-mode' and 'delivery-persistent' attributes are mutually exclusive.", element);
			return null;
		}
		if (StringUtils.hasText(deliveryMode)) {
			parserContext.getReaderContext().warning(
					"The 'delivery-mode' attribute is deprecated. Use 'delivery-persistent' instead.", element);
			builder.addPropertyValue("deliveryMode", deliveryMode);
		}
		else if (StringUtils.hasText(deliveryPersistent)) {
			builder.addPropertyValue("deliveryPersistent", deliveryPersistent);
		}
		return builder;
	}

}
