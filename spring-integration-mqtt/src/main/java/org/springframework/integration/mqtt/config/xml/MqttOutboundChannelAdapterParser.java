/*
   * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.mqtt.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.util.StringUtils;

/**
 * The parser for the MqttAdapter Outbound Channel Adapter.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public class MqttOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {

		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MqttPahoMessageHandler.class);

		MqttParserUtils.parseCommon(element, builder, parserContext);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-topic");
		if (StringUtils.hasText(element.getAttribute("converter")) &&
				(StringUtils.hasText(element.getAttribute("default-qos")) ||
				 StringUtils.hasText(element.getAttribute("default-retained")))) {
			parserContext.getReaderContext().error("If a 'converter' is provided, you cannot provide " +
					"'default-qos' or 'default-retained'", element);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-qos");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-retained");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "async");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "async-events");

		return builder.getBeanDefinition();

	}

}
