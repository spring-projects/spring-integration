/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;splitter/&gt; element.
 * 
 * @author Mark Fisher
 */
public class SplitterParser extends AbstractEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseConsumer(Element element, ParserContext parserContext) {
		if (element.hasAttribute(REF_ATTRIBUTE)) {
			String ref = element.getAttribute(REF_ATTRIBUTE);
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingSplitter.class);
			builder.addConstructorArgReference(ref);
			if (StringUtils.hasText(element.getAttribute(METHOD_ATTRIBUTE))) {
				String method = element.getAttribute(METHOD_ATTRIBUTE);
				builder.addConstructorArgValue(method);
			}
			return builder;
		}
		return BeanDefinitionBuilder.genericBeanDefinition(DefaultMessageSplitter.class);
	}

}
