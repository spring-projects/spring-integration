/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.scripting.config;

import org.springframework.integration.scripting.AbstractScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.scripting.jsr223.ScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.jsr223.ScriptExecutorFactory;
import org.springframework.scripting.ScriptSource;

/**
 * The factory to create {@link AbstractScriptExecutingMessageProcessor} instances for provided arguments.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public class ScriptExecutingProcessorFactory {

	public static final String BEAN_NAME = "integrationScriptExecutingProcessorFactory";

	public AbstractScriptExecutingMessageProcessor<?> createMessageProcessor(String language,
			ScriptSource scriptSource, ScriptVariableGenerator scriptVariableGenerator) {
		ScriptExecutor scriptExecutor = ScriptExecutorFactory.getScriptExecutor(language);
		return new ScriptExecutingMessageProcessor(scriptSource, scriptVariableGenerator, scriptExecutor);
	}

}
