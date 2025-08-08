/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.config.AbstractScriptParser;

/**
 * Parser for the &lt;groovy:script/&gt; element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Artem Bilan
 * @since 2.0
 */
public class GroovyScriptParser extends AbstractScriptParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return GroovyScriptExecutingMessageProcessor.class;
	}

	protected void postProcess(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "customizer");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "compiler-configuration");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "compile-static");
	}

}
