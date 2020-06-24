/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.kafka.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.AbstractInboundGatewayParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.inbound.KafkaInboundGateway;

/**
 * Inbound gateway parser.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class KafkaInboundGatewayParser extends AbstractInboundGatewayParser {


	@Override
	protected Class<?> getBeanClass(Element element) {
		return KafkaInboundGateway.class;
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		builder.addConstructorArgReference(element.getAttribute("listener-container"));
		builder.addConstructorArgReference(element.getAttribute("kafka-template"));
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-message-strategy");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "retry-template");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "recovery-callback");
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return super.isEligibleAttribute(attributeName)
				&& !attributeName.equals("listener-container")
				&& !attributeName.equals("kafka-template");
	}

}
