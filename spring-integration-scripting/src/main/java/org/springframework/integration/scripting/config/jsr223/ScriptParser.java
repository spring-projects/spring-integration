/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
