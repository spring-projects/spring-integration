/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.activiti.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.activiti.adapter.ActivitiOutboundChannelAdapter;
import org.springframework.integration.activiti.gateway.ActivityBehaviorMessagingGatewayFactoryBean;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.w3c.dom.Element;


/**
 * Provides namespace support for defining a Gateway that communicates <em>from</em> Acitviti <em>to</em> Spring Integration.
 *
 * @author Josh Long
 */
@SuppressWarnings("unused")
public class ActivitiNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {
		this.registerBeanDefinitionParser("inbound-gateway", new ActivitiInboundGatewayParser());
		this.registerBeanDefinitionParser("outbound-channel-adapter", new ActivitiOutboundChannelAdapterParser());
	}

	private static class ActivitiOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {
		@Override
		protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ActivitiOutboundChannelAdapter.class.getName());
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "process-engine");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "process-definition-name");
			return builder.getBeanDefinition();
		}
	}

	private static class ActivitiInboundGatewayParser extends AbstractSingleBeanDefinitionParser {
		@Override
		protected String getBeanClassName(Element element) {
			return ActivityBehaviorMessagingGatewayFactoryBean.class.getName();
		}

		@Override
		protected boolean shouldGenerateIdAsFallback() {
			return true;
		}

		@Override
		protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "process-engine");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "update-process-variables-from-reply-message-headers");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "async");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "forward-process-variables-as-message-headers");
		}
	}
}
