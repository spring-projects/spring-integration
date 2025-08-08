/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.MapToObjectTransformer;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class MapToObjectTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return MapToObjectTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String ref = element.getAttribute("ref");
		String type = element.getAttribute("type");
		if (StringUtils.hasText(ref) && StringUtils.hasText(type)) {
			parserContext.getReaderContext().error("'type' and 'ref' attributes are mutually-exclusive, " +
							"but both have valid values; type: " + type + "; ref: " + ref,
					IntegrationNamespaceUtils.createElementDescription(element));
		}
		if (StringUtils.hasText(ref)) {
			builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(ref, "java.lang.String");
		}
		else if (StringUtils.hasText(type)) {
			builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(type, "java.lang.Class");
		}
	}

}
