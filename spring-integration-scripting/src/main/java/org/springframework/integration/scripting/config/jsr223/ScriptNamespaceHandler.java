/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
