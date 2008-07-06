/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.DefaultMessageHandlerAdapter;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 */
public class DefaultHandlerEndpointParser extends AbstractHandlerEndpointParser {

	private static final String REPLY_HANDLER_ATTRIBUTE = "reply-handler";

	private static final String REPLY_HANDLER_PROPERTY = "replyHandler";


	@Override
	protected Class<? extends MessageHandler> getHandlerAdapterClass() {
		return DefaultMessageHandlerAdapter.class;
	}

	@Override
	protected void postProcessEndpointBean(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		String replyHandler = element.getAttribute(REPLY_HANDLER_ATTRIBUTE);
		if (StringUtils.hasText(replyHandler)) {
			builder.addPropertyValue(REPLY_HANDLER_PROPERTY, new RuntimeBeanReference(replyHandler));
		}
	}

}
