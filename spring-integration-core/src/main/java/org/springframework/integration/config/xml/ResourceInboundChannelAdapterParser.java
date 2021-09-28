/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.resource.ResourceRetrievingMessageSource;
import org.springframework.integration.util.AcceptOnceCollectionFilter;
import org.springframework.util.StringUtils;

/**
 * Parser for 'resource-inbound-channel-adapter'.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.1
 */
public class ResourceInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	private static final String FILTER = "filter";

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder sourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(ResourceRetrievingMessageSource.class);
		sourceBuilder.addConstructorArgValue(element.getAttribute("pattern"));
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(sourceBuilder, element, "pattern-resolver");
		boolean hasFilter = element.hasAttribute(FILTER);
		if (hasFilter) {
			String filterValue = element.getAttribute(FILTER);
			if (StringUtils.hasText(filterValue)) {
				sourceBuilder.addPropertyReference(FILTER, filterValue);
			}
		}
		else {
			BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(AcceptOnceCollectionFilter.class);
			sourceBuilder.addPropertyValue(FILTER, filterBuilder.getBeanDefinition());
		}
		return sourceBuilder.getBeanDefinition();
	}

}
