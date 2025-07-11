/*
 * Copyright 2013-present the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.SpelPropertyAccessorRegistrar;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;spel-property-accessors&gt; element.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Christian Tzolov
 *
 * @since 3.0
 */
public class SpelPropertyAccessorsParser implements BeanDefinitionParser {

	@Override
	public @Nullable BeanDefinition parse(Element element, ParserContext parserContext) {
		Map<String, Object> propertyAccessors = new ManagedMap<>();
		Map<String, Object> indexAccessors = new ManagedMap<>();
		parseTargetedAccessors(element, parserContext, propertyAccessors);

		Element indexAccessorsElement = DomUtils.getChildElementByTagName(element, "index-accessors");
		if (indexAccessorsElement != null) {
			parseTargetedAccessors(indexAccessorsElement, parserContext, indexAccessors);
		}

		BeanDefinitionBuilder registrarBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(SpelPropertyAccessorRegistrar.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		if (!CollectionUtils.isEmpty(propertyAccessors)) {
			registrarBuilder.addConstructorArgValue(propertyAccessors);
		}

		if (!CollectionUtils.isEmpty(indexAccessors)) {
			registrarBuilder.addPropertyValue("indexAccessors", indexAccessors);
		}

		parserContext.getRegistry()
				.registerBeanDefinition(IntegrationContextUtils.SPEL_PROPERTY_ACCESSOR_REGISTRAR_BEAN_NAME,
						registrarBuilder.getBeanDefinition());

		return null;
	}

	private static void parseTargetedAccessors(Element accessorsElement, ParserContext parserContext,
			Map<String, Object> accessorsMap) {

		BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
		List<Element> accessorElements = DomUtils.getChildElementsByTagName(accessorsElement,
				BeanDefinitionParserDelegate.BEAN_ELEMENT, BeanDefinitionParserDelegate.REF_ELEMENT);
		for (Element accessorElement : accessorElements) {
			String accessorName;
			Object accessor;
			if (delegate.nodeNameEquals(accessorElement, BeanDefinitionParserDelegate.BEAN_ELEMENT)) {
				accessorName = accessorElement.getAttribute(BeanDefinitionParserDelegate.ID_ATTRIBUTE);
				if (!StringUtils.hasText(accessorName)) {
					parserContext.getReaderContext()
							.error("The '<bean>' 'id' attribute is required within 'spel-property-accessors'.",
									accessorElement);
					return;
				}
				accessor = delegate.parseBeanDefinitionElement(accessorElement);
			}
			else {
				BeanReference propertyAccessorRef =
						(BeanReference) delegate.parsePropertySubElement(accessorElement, null);
				accessorName = Objects.requireNonNull(propertyAccessorRef).getBeanName();
				accessor = propertyAccessorRef;
			}
			accessorsMap.put(accessorName, accessor);
		}
	}

}
