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

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
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

	}

}
