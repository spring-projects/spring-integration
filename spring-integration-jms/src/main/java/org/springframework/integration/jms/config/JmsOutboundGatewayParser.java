/*
 * Copyright 2002-2010 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
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
	private static final Log logger = LogFactory.getLog(JmsOutboundGatewayParser.class);

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
		if (!(StringUtils.hasText(requestDestination) ^ StringUtils.hasText(requestDestinationName))) {
			parserContext.getReaderContext().error(
					"Exactly one of the 'request-destination' or 'request-destination-name' attributes is required.", element);
		}
		if (StringUtils.hasText(requestDestination)) {
			builder.addPropertyReference("requestDestination", requestDestination);
		}
		else if (StringUtils.hasText(requestDestinationName)) {
			builder.addPropertyValue("requestDestinationName", requestDestinationName);
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
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "pub-sub-domain");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "time-to-live");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "priority");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "explicit-qos-enabled");
		
		String deliveryMode = element.getAttribute("delivery-mode");
		String deliveryPersistent = element.getAttribute("delivery-persistent");
		
		if (StringUtils.hasText(deliveryMode) && StringUtils.hasText(deliveryPersistent)){
			parserContext.getReaderContext().
				error("Exactly one of the 'delivery-mode' attribute or 'delivery-persistent' attribute is allowed", element);
			return null;
		}
		if (StringUtils.hasText(deliveryMode)){
			logger.warn("'delivery-mode' attribute is deprecated. Use 'delivery-persistent' instead");
			builder.addPropertyValue("deliveryMode", deliveryMode);
		}
		else if (StringUtils.hasText(deliveryPersistent)){
			builder.addPropertyValue("deliveryPersistent", deliveryPersistent);
		}
		return builder;
	}

}
