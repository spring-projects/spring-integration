/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.config.EnablePublisher;
import org.springframework.integration.config.IntegrationRegistrar;
import org.springframework.integration.config.PublisherRegistrar;
import org.springframework.integration.config.annotation.AnnotationMetadataAdapter;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the {@code <annotation-config>} element of the integration namespace.
 * Delegates the real configuration to the {@link IntegrationRegistrar}.
 * If {@code <enable-publisher>} sub-element is present, the {@link PublisherRegistrar}
 * is called, too.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public class AnnotationConfigParser implements BeanDefinitionParser {

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		ExtendedAnnotationMetadata importingClassMetadata = new ExtendedAnnotationMetadata(element);
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		new IntegrationRegistrar()
				.registerBeanDefinitions(importingClassMetadata, registry);
		if (DomUtils.getChildElementByTagName(element, "enable-publisher") != null) {
			new PublisherRegistrar()
					.registerBeanDefinitions(importingClassMetadata, registry);
		}
		return null;
	}

	private static final class ExtendedAnnotationMetadata extends AnnotationMetadataAdapter {

		private final Element element;

		ExtendedAnnotationMetadata(Element element) {
			this.element = element;
		}

		@Override
		public Map<String, Object> getAnnotationAttributes(String annotationType) {
			if (EnablePublisher.class.getName().equals(annotationType)) {
				Element enablePublisherElement = DomUtils.getChildElementByTagName(this.element, "enable-publisher");
				if (enablePublisherElement != null) {
					Map<String, Object> attributes = new HashMap<>();
					attributes.put("defaultChannel", enablePublisherElement.getAttribute("default-publisher-channel"));
					attributes.put("proxyTargetClass", enablePublisherElement.getAttribute("proxy-target-class"));
					attributes.put("order", enablePublisherElement.getAttribute("order"));
					return attributes;
				}
				else {
					return null;
				}
			}
			else {
				return null;
			}
		}

		@Override
		public Set<MethodMetadata> getDeclaredMethods() {
			throw new UnsupportedOperationException("The class doesn't support this operation");
		}

	}

}
