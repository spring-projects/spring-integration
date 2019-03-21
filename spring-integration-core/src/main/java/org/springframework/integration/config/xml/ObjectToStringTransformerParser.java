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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'object-to-string-transformer' element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class ObjectToStringTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return ObjectToStringTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String charset = element.getAttribute("charset");
		if (StringUtils.hasText(charset)) {
			builder.addConstructorArgValue(charset);
		}
	}

}
