/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.gemfire.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.gemfire.inbound.ContinuousQueryMessageProducer;

/**
 * @author David Turanski
 * @author Dan Oxlade
 * @author Gary Russell
 * @since 2.1
 *
 */
public class GemfireCqInboundChannelAdapterParser extends AbstractChannelAdapterParser {


	private static final String ERROR_CHANNEL_ATTRIBUTE = "error-channel";

	private static final String OUTPUT_CHANNEL_PROPERTY = "outputChannel";

	private static final String QUERY_LISTENER_CONTAINER_ATTRIBUTE = "cq-listener-container";

	private static final String DURABLE_ATTRIBUTE = "durable";

	private static final String QUERY_NAME_ATTRIBUTE = "query-name";

	private static final String QUERY_ATTRIBUTE = "query";

	private static final String PAYLOAD_EXPRESSION_PROPERTY = "payloadExpression";

	private static final String EXPRESSION_ATTRIBUTE = "expression";

	private static final String SUPPORTED_EVENT_TYPES_PROPERTY = "supportedEventTypes";

	private static final String QUERY_EVENTS_ATTRIBUTE = "query-events";

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder continuousQueryMesageProducer =
				BeanDefinitionBuilder.genericBeanDefinition(ContinuousQueryMessageProducer.class);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(continuousQueryMesageProducer, element,
				EXPRESSION_ATTRIBUTE, PAYLOAD_EXPRESSION_PROPERTY);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(continuousQueryMesageProducer, element,
				QUERY_EVENTS_ATTRIBUTE, SUPPORTED_EVENT_TYPES_PROPERTY);

		if (!element.hasAttribute(QUERY_LISTENER_CONTAINER_ATTRIBUTE)) {
			parserContext.getReaderContext()
					.error("'" + QUERY_LISTENER_CONTAINER_ATTRIBUTE + "' attribute is required.", element);
		}

		if (!element.hasAttribute(QUERY_ATTRIBUTE)) {
			parserContext.getReaderContext().error("'" + QUERY_ATTRIBUTE + "' attribute is required.", element);
		}

		continuousQueryMesageProducer.addConstructorArgReference(element.getAttribute(QUERY_LISTENER_CONTAINER_ATTRIBUTE));
		continuousQueryMesageProducer.addConstructorArgValue(element.getAttribute(QUERY_ATTRIBUTE));

		continuousQueryMesageProducer.addPropertyReference(OUTPUT_CHANNEL_PROPERTY, channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(continuousQueryMesageProducer, element,
				ERROR_CHANNEL_ATTRIBUTE);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(continuousQueryMesageProducer, element, QUERY_NAME_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(continuousQueryMesageProducer, element, DURABLE_ATTRIBUTE);
		return continuousQueryMesageProducer.getBeanDefinition();
	}

}
