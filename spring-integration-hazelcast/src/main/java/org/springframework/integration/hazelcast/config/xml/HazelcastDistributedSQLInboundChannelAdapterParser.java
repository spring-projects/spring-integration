/*
 * Copyright 2015-2024 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.hazelcast.inbound.HazelcastDistributedSQLMessageSource;
import org.springframework.util.StringUtils;

/**
 * Hazelcast Distributed SQL Inbound Channel Adapter Parser parses
 * {@code <int-hazelcast:ds-inbound-channel-adapter/>} configuration.
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
public class HazelcastDistributedSQLInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	private static final String CACHE_ATTRIBUTE = "cache";

	private static final String DISTRIBUTED_SQL_ATTRIBUTE = "distributed-sql";

	private static final String ITERATION_TYPE_ATTRIBUTE = "iteration-type";

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		if (!StringUtils.hasText(element.getAttribute(CACHE_ATTRIBUTE))) {
			parserContext.getReaderContext().error("'" + CACHE_ATTRIBUTE + "' attribute is required.", element);
		}
		else if (!StringUtils.hasText(element.getAttribute(DISTRIBUTED_SQL_ATTRIBUTE))) {
			parserContext.getReaderContext().error("'" + DISTRIBUTED_SQL_ATTRIBUTE + "' attribute is required.",
					element);
		}
		else if (!StringUtils.hasText(element.getAttribute(ITERATION_TYPE_ATTRIBUTE))) {
			parserContext.getReaderContext().error("'" + ITERATION_TYPE_ATTRIBUTE + "' attribute is required.",
					element);
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(HazelcastDistributedSQLMessageSource.class.getName());

		builder.addConstructorArgReference(element.getAttribute(CACHE_ATTRIBUTE));
		builder.addConstructorArgValue(element.getAttribute(DISTRIBUTED_SQL_ATTRIBUTE));

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, ITERATION_TYPE_ATTRIBUTE);

		return builder.getBeanDefinition();
	}

}
