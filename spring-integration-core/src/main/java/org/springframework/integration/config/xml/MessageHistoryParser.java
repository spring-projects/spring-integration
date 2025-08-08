/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.config.MessageHistoryRegistrar;
import org.springframework.integration.config.annotation.AnnotationMetadataAdapter;

/**
 * The {@code <message-history/>} parser.
 * Delegates the {@link BeanDefinition} registration to the {@link MessageHistoryRegistrar}.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MessageHistoryParser implements BeanDefinitionParser {

	private final MessageHistoryRegistrar messageHistoryRegistrar = new MessageHistoryRegistrar();

	@Override
	public BeanDefinition parse(final Element element, ParserContext parserContext) {
		this.messageHistoryRegistrar.registerBeanDefinitions(
				new AnnotationMetadataAdapter() {

					@Override
					public Map<String, Object> getAnnotationAttributes(String annotationType) {
						return Collections.singletonMap("value", element.getAttribute("tracked-components"));
					}

					@Override
					public Set<MethodMetadata> getDeclaredMethods() {
						throw new UnsupportedOperationException("The class doesn't support this operation");
					}

				}, parserContext.getRegistry());
		return null;
	}

}
