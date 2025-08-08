/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.AbstractScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.scripting.config.ScriptExecutingProcessorFactory;
import org.springframework.scripting.ScriptSource;

/**
 * The factory to create {@link GroovyScriptExecutingMessageProcessor} instances
 * if provided {@code language == "groovy"}, otherwise delegates to the super class.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class GroovyAwareScriptExecutingProcessorFactory extends ScriptExecutingProcessorFactory {

	@Override
	public AbstractScriptExecutingMessageProcessor<?> createMessageProcessor(String language,
			ScriptSource scriptSource, ScriptVariableGenerator scriptVariableGenerator) {

		if ("groovy".equalsIgnoreCase(language)) {
			return new GroovyScriptExecutingMessageProcessor(scriptSource, scriptVariableGenerator);
		}
		else {
			return super.createMessageProcessor(language, scriptSource, scriptVariableGenerator);
		}
	}

}
