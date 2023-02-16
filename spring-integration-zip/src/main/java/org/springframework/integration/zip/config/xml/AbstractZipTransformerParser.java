/*
 * Copyright 2015-2023 the original author or authors.
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

package org.springframework.integration.zip.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for Zip transformer parsers.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 6.1
 */
public abstract class AbstractZipTransformerParser extends AbstractTransformerParser {

	/**
	 * @param element The XML Element to process
	 * @param parserContext The Spring ParserContext
	 * @param builder BeanDefinitionBuilder for constructing Bean Definitions
	 */
	@Override
	protected final void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String deleteFiles = element.getAttribute("delete-files");
		if (StringUtils.hasText(deleteFiles)) {
			builder.addPropertyValue("deleteFiles", deleteFiles);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "result-type", "zipResultType");
		postProcessTransformer(element, parserContext, builder);
	}

	/**
	 * Subclasses may override this method to provide additional configuration.
	 *
	 * @param element The XML Element to process
	 * @param parserContext The Spring ParserContext
	 * @param builder BeanDefinitionBuilder for constructing Bean Definitions
	 */
	protected void postProcessTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
	}

}
