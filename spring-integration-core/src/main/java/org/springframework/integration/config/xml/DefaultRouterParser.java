/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.RouterFactoryBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;router/&gt; element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class DefaultRouterParser extends AbstractDelegatingConsumerEndpointParser {

	@Override
	String getFactoryBeanClassName() {
		return RouterFactoryBean.class.getName();
	}

	@Override
	boolean hasDefaultOption() {
		return false;
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		List<Element> mappingElements = DomUtils.getChildElementsByTagName(element, "mapping");
		if (!CollectionUtils.isEmpty(mappingElements)) {
			ManagedMap<String, String> channelMappings = new ManagedMap<>();
			for (Element mappingElement : mappingElements) {
				channelMappings.put(mappingElement.getAttribute("value"), mappingElement.getAttribute("channel"));
			}
			builder.addPropertyValue("channelMappings", channelMappings);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "default-output-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "resolution-required");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "apply-sequence");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-send-failures");
	}

}
