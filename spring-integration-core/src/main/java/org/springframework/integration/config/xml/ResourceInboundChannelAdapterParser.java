/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
