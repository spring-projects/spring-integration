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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.router.HeaderValueRouter;

/**
 * Parser for the &lt;header-value-router/&gt; element.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 1.0.3
 */
public class HeaderValueRouterParser extends AbstractRouterParser {

	@Override
	protected BeanDefinition doParseRouter(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder headerValueRouterBuilder = BeanDefinitionBuilder.genericBeanDefinition(HeaderValueRouter.class);
		headerValueRouterBuilder.addConstructorArgValue(element.getAttribute("header-name"));
		return headerValueRouterBuilder.getBeanDefinition();
	}

}
