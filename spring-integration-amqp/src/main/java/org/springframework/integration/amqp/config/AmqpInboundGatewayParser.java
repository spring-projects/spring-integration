/*
 * Copyright 2002-2024 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.amqp.inbound.AmqpInboundGateway;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the AMQP 'inbound-gateway' element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class AmqpInboundGatewayParser extends AbstractAmqpInboundAdapterParser {

	AmqpInboundGatewayParser() {
		super(AmqpInboundGateway.class.getName());
	}

	@Override
	protected void configureChannels(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String amqpTemplateRef = element.getAttribute("amqp-template");
		if (StringUtils.hasText(amqpTemplateRef)) {
			builder.addConstructorArgReference(amqpTemplateRef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-reply-to");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-headers-last",
				"replyHeadersMappedLast");
	}

}
