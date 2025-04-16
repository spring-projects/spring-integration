/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.integration.hazelcast.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.hazelcast.inbound.HazelcastClusterMonitorMessageProducer;
import org.springframework.util.StringUtils;

/**
 * Parser for the {@code <int-hazelcast:cm-inbound-channel-adapter />} component.
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
public class HazelcastClusterMonitorInboundChannelAdapterParser extends
		AbstractSingleBeanDefinitionParser {

	private static final String CHANNEL_ATTRIBUTE = "channel";

	private static final String HAZELCAST_INSTANCE_ATTRIBUTE = "hazelcast-instance";

	private static final String MONITOR_TYPES_ATTRIBUTE = "monitor-types";

	private static final String OUTPUT_CHANNEL = "outputChannel";

	private static final String MONITOR_EVENT_TYPES = "monitorEventTypes";

	@Override
	protected Class<?> getBeanClass(Element element) {
		return HazelcastClusterMonitorMessageProducer.class;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition,
			ParserContext parserContext) throws BeanDefinitionStoreException {
		String id = super.resolveId(element, definition, parserContext);

		if (!element.hasAttribute(CHANNEL_ATTRIBUTE)) {
			id = id + ".adapter";
		}

		if (!StringUtils.hasText(id)) {
			id = BeanDefinitionReaderUtils.generateBeanName(definition,
					parserContext.getRegistry());
		}

		return id;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		String channelName = element.getAttribute(CHANNEL_ATTRIBUTE);
		if (!StringUtils.hasText(channelName)) {
			channelName = IntegrationNamespaceUtils.createDirectChannel(element,
					parserContext);
		}

		if (!StringUtils.hasText(element.getAttribute(HAZELCAST_INSTANCE_ATTRIBUTE))) {
			parserContext.getReaderContext().error(
					"'" + HAZELCAST_INSTANCE_ATTRIBUTE + "' attribute is required.",
					element);
		}

		builder.addPropertyReference(OUTPUT_CHANNEL, channelName);
		builder.addConstructorArgReference(element
				.getAttribute(HAZELCAST_INSTANCE_ATTRIBUTE));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				MONITOR_TYPES_ATTRIBUTE, MONITOR_EVENT_TYPES);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IntegrationNamespaceUtils.AUTO_STARTUP);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IntegrationNamespaceUtils.PHASE);
	}

}
