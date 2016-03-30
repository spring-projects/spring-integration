/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.scripting.config.jsr223;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.scripting.config.AbstractScriptParser;
import org.springframework.integration.scripting.jsr223.ScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.jsr223.ScriptExecutorFactory;
import org.springframework.util.StringUtils;

/**
 * @author David Turanski
 * @author Artem Bilan
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
				return;
			}
			else {
				language = getLanguageFromFileExtension(scriptLocation, parserContext, element);
				if (language == null) {
					parserContext.getReaderContext().error(
							"Unable to determine language for script '" + scriptLocation + "'", element);
					return;
				}
			}
		}

		builder.addConstructorArgValue(ScriptExecutorFactory.getScriptExecutor(language));
	}

	private String getLanguageFromFileExtension(String scriptLocation, ParserContext parserContext, Element element) {
		ScriptEngineManager engineManager = new ScriptEngineManager();
		ScriptEngine engine = null;

		int index = scriptLocation.lastIndexOf(".") + 1;
		if (index < 1) {
			return null;
		}
		String extension = scriptLocation.substring(index);

		engine = engineManager.getEngineByExtension(extension);

		if (engine == null) {
			parserContext.getReaderContext().error(
					"No suitable scripting engine found for extension '" + extension + "'", element);
		}

		return engine.getFactory().getLanguageName();
	}
}
