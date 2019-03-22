/*
 * Copyright 2002-2019 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.ParserContext;

/**
 * Simple strategy interface for parsers that are responsible
 * for parsing an element, creating a bean definition, and then
 * registering the bean. The {@link #parse(Element, ParserContext)}
 * method should return the name of the registered bean.
 *
 * @author Mark Fisher
 */
public interface BeanDefinitionRegisteringParser {

	String parse(Element element, ParserContext parserContext);

}
