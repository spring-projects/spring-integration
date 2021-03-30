/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.http.config;

import java.util.Collections;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
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

				}, parserContext.getRegistry());

		return null;
	}

}
