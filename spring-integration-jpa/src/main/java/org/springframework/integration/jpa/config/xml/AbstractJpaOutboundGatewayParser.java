/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.jpa.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.outbound.JpaOutboundGatewayFactoryBean;
import org.springframework.util.StringUtils;

/**
 * The Abstract Parser for the JPA Outbound Gateways.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 * @see RetrievingJpaOutboundGatewayParser
 * @see UpdatingJpaOutboundGatewayParser
 *
 */
public abstract class AbstractJpaOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element gatewayElement, ParserContext parserContext) {
		final BeanDefinitionBuilder jpaOutboundGatewayBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(JpaOutboundGatewayFactoryBean.class);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaOutboundGatewayBuilder, gatewayElement, "reply-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaOutboundGatewayBuilder, gatewayElement, "requires-reply");

		String replyChannel = gatewayElement.getAttribute("reply-channel");

		if (StringUtils.hasText(replyChannel)) {
			jpaOutboundGatewayBuilder.addPropertyReference("outputChannel", replyChannel);
		}

		return jpaOutboundGatewayBuilder;

	}

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

}
