/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.xmpp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * Parser for the XMPP 'inbound-channel-adapter' element.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class ChatMessageInboundChannelAdapterParser extends AbstractXmppInboundChannelAdapterParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.integration.xmpp.inbound.ChatMessageListeningEndpoint";
	}

	@Override
	protected void postProcess(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		BeanDefinition expression =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("payload-expression", element);
		if (expression != null) {
			builder.addPropertyValue("payloadExpression", expression);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "stanza-filter");
	}

}
