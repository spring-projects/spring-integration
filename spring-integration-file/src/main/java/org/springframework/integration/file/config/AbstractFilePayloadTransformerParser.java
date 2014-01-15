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

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.util.StringUtils;

/**
 * Base class for File payload transformer parsers.
 *
 * @author Mark Fisher
 */
public abstract class AbstractFilePayloadTransformerParser extends AbstractTransformerParser {

	@Override
	protected final void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String deleteFiles = element.getAttribute("delete-files");
		if (StringUtils.hasText(deleteFiles)) {
			builder.addPropertyValue("deleteFiles", deleteFiles);
		}
		this.postProcessTransformer(element, parserContext, builder);
	}

	/**
	 * Subclasses may override this method to provide additional configuration.
	 *
	 * @param element The element.
	 * @param parserContext The parser context.
	 * @param builder The builder.
	 */
	protected void postProcessTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
	}

}
