/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class GroovyNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("script", new GroovyScriptParser());
	}

}
