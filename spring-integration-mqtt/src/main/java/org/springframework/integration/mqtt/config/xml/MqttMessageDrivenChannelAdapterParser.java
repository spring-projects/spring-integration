/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.mqtt.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;

/**
 * The MqttAdapter Message Driven Channel adapter parser
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.0
 *
 */
public class MqttMessageDrivenChannelAdapterParser extends AbstractChannelAdapterParser {


	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(MqttPahoMessageDrivenChannelAdapter.class);

		MqttParserUtils.parseCommon(element, builder, parserContext);
		builder.addConstructorArgValue(element.getAttribute("topics"));
		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "qos");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "recovery-interval");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "manual-acks");

		return builder.getBeanDefinition();
	}

}
