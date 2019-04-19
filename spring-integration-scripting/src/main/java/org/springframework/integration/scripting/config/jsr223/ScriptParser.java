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

package org.springframework.integration.scripting.config.jsr223;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.scripting.config.AbstractScriptParser;
import org.springframework.integration.scripting.jsr223.ScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.jsr223.ScriptExecutorFactory;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractScriptParser} parser extension for the {@code <int-script:script>} tag.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class ScriptParser extends AbstractScriptParser {

	private static final String LANGUAGE_ATTRIBUTE = "lang";

	@Override
	protected Class<?> getBeanClass(Element element) {
		return ScriptExecutingMessageProcessor.class;
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		String language = element.getAttribute(LANGUAGE_ATTRIBUTE);
		String scriptLocation = element.getAttribute(LOCATION_ATTRIBUTE);
		if (!StringUtils.hasText(language)) {
			if (!StringUtils.hasText(scriptLocation)) {
				parserContext.getReaderContext().error(
						"An inline script requires the '" + LANGUAGE_ATTRIBUTE + "' attribute.", element);
			}
			else {
				language = ScriptExecutorFactory.deriveLanguageFromFileExtension(scriptLocation);
			}
		}
		builder.addConstructorArgValue(ScriptExecutorFactory.getScriptExecutor(language));
	}

}
