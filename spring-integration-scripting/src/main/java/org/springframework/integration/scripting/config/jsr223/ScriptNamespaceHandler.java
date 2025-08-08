/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.config.jsr223;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author David Turanski
 * @since 2.1
 */
public class ScriptNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("script", new ScriptParser());
	}

}
