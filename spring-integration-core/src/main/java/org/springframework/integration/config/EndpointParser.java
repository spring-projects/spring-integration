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
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.handler.DefaultMessageHandlerAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>handler-endpoint</em> element of the integration namespace.
 * 
 * @author Mark Fisher
 */
public class EndpointParser extends AbstractTargetEndpointParser {

	private static final String OUTPUT_CHANNEL_ATTRIBUTE = "output-channel";

	private static final String DEFAULT_OUTPUT_CHANNEL_PROPERTY = "defaultOutputChannelName";

	private static final String RETURN_ADDRESS_OVERRIDES_ATTRIBUTE = "return-address-overrides";

	private static final String REPLY_HANDLER_ATTRIBUTE = "reply-handler";

	private static final String REPLY_HANDLER_PROPERTY = "replyHandler";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return HandlerEndpoint.class;
	}

	protected String getTargetAttributeName() {
		return "handler";
	}

	@Override
	protected Class<?> getAdapterClass() {
		return DefaultMessageHandlerAdapter.class;
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element) {
		String outputChannel = element.getAttribute(OUTPUT_CHANNEL_ATTRIBUTE);
		if (StringUtils.hasText(outputChannel)) {
			builder.addPropertyValue(DEFAULT_OUTPUT_CHANNEL_PROPERTY, outputChannel);
		}
		String returnAddressOverridesAttribute = element.getAttribute(RETURN_ADDRESS_OVERRIDES_ATTRIBUTE);
		boolean returnAddressOverrides = "true".equals(returnAddressOverridesAttribute);
		builder.addPropertyValue("returnAddressOverrides", returnAddressOverrides);
		String replyHandler = element.getAttribute(REPLY_HANDLER_ATTRIBUTE);
		if (StringUtils.hasText(replyHandler)) {
			builder.addPropertyValue(REPLY_HANDLER_PROPERTY, new RuntimeBeanReference(replyHandler));
		}
	}

}
