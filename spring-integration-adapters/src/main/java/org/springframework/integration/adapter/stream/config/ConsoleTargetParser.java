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

package org.springframework.integration.adapter.stream.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.integration.adapter.stream.CharacterStreamTargetAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;console-target&gt; element.
 * 
 * @author Mark Fisher
 */
public class ConsoleTargetParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return CharacterStreamTargetAdapter.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		if ("true".equals(element.getAttribute("error"))) {
			builder.setFactoryMethod("stderrAdapter");
		}
		else {
			builder.setFactoryMethod("stdoutAdapter");
		}
		String charsetName = element.getAttribute("charset");
		if (StringUtils.hasText(charsetName)) {
			builder.addConstructorArgValue(charsetName);
		}
	}

}
