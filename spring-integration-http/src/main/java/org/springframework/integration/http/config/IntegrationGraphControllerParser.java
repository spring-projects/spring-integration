/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.http.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.config.annotation.AnnotationMetadataAdapter;

/**
 * The {@link BeanDefinitionParser} for the {@code <int-http:graph-controller>} component.
 *
 * @author Artem Bilan
 *
 * @since 4.3
 */
public class IntegrationGraphControllerParser implements BeanDefinitionParser {

	private final IntegrationGraphControllerRegistrar graphControllerRegistrar =
			new IntegrationGraphControllerRegistrar();

	@Override
	public BeanDefinition parse(final Element element, ParserContext parserContext) {
		this.graphControllerRegistrar.registerBeanDefinitions(
				new AnnotationMetadataAdapter() {

					@Override
					public Map<String, Object> getAnnotationAttributes(String annotationType) {
						return Collections.singletonMap("value", element.getAttribute("path"));
					}

					@Override
					public Set<MethodMetadata> getDeclaredMethods() {
						throw new UnsupportedOperationException("The class doesn't support this operation");
					}

				}, parserContext.getRegistry());

		return null;
	}

}
