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

package org.springframework.integration.xmpp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.xmpp.outbound.ChatMessageSendingMessageHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for the XMPP 'outbound-channel-adapter' element
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ChatMessageOutboundChannelAdapterParser extends AbstractXmppOutboundChannelAdapterParser {

	@Override
	protected String getHandlerClassName() {
		return ChatMessageSendingMessageHandler.class.getName();
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		AbstractBeanDefinition beanDefinition = super.parseConsumer(element, parserContext);
		String extensionProvider = element.getAttribute("extension-provider");
		if (StringUtils.hasText(extensionProvider)) {
			beanDefinition.getPropertyValues()
					.addPropertyValue("extensionProvider", new RuntimeBeanReference(extensionProvider));
		}
		return beanDefinition;
	}

}
