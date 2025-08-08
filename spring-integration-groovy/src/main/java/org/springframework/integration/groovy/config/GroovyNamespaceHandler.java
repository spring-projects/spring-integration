/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class GroovyNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("script", new GroovyScriptParser());
		this.registerBeanDefinitionParser("control-bus", new GroovyControlBusParser());
	}

}
