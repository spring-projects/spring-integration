/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import java.util.Collections;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.integration.config.IntegrationRegistrar;

/**
 * Parser for the &lt;annotation-config&gt; element of the integration namespace.
 * Just delegate the real configuration to the {@link IntegrationRegistrar}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class AnnotationConfigParser implements BeanDefinitionParser {

	public BeanDefinition parse(final Element element, ParserContext parserContext) {
		new IntegrationRegistrar().registerBeanDefinitions(new StandardAnnotationMetadata(Object.class) {

			@Override
			public Map<String, Object> getAnnotationAttributes(String annotationType) {
				return Collections.<String, Object> singletonMap("value", element.getAttribute("default-publisher-channel"));
			}
		}, parserContext.getRegistry());

		return null;
	}

}
