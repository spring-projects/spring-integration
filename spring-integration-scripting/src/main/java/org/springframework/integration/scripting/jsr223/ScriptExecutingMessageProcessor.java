/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.jsr223;

import java.util.Map;

import org.springframework.integration.scripting.AbstractScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.DefaultScriptVariableGenerator;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.messaging.Message;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * An {@link AbstractScriptExecutingMessageProcessor} implementation for evaluating scripts
 * from the provided {@link ScriptSource} in the provided {@link ScriptExecutor} against an optional
 * binding {@code variables}.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class ScriptExecutingMessageProcessor extends AbstractScriptExecutingMessageProcessor<Object> {

	private final ScriptExecutor scriptExecutor;

	private final ScriptSource scriptSource;

	/**
	 * Create a processor for the {@link ScriptSource} using the provided
	 * {@link ScriptExecutor} using the DefaultScriptVariableGenerator
	 * @param scriptSource The script source.
	 * @param scriptExecutor The script executor.
	 */
	public ScriptExecutingMessageProcessor(ScriptSource scriptSource, ScriptExecutor scriptExecutor) {
		this(scriptSource, new DefaultScriptVariableGenerator(), scriptExecutor);
	}

	/**
	 * Create a processor for the {@link ScriptSource} using the provided
	 * {@link ScriptExecutor}
	 * @param scriptSource The script source.
	 * @param scriptVariableGenerator The script variable generator.
	 * @param scriptExecutor The script executor.
	 */
	public ScriptExecutingMessageProcessor(ScriptSource scriptSource, ScriptVariableGenerator scriptVariableGenerator,
			ScriptExecutor scriptExecutor) {

		super(scriptVariableGenerator);
		Assert.notNull(scriptSource, "'scriptSource' must not be null");
		Assert.notNull(scriptExecutor, "'scriptExecutor' must not be null");
		this.scriptSource = scriptSource;
		this.scriptExecutor = scriptExecutor;
	}

	/**
	 * Create a processor for the {@link ScriptSource} using the provided
	 * {@link ScriptExecutor} using the DefaultScriptVariableGenerator
	 * @param scriptSource The script source.
	 * @param scriptExecutor The script executor.
	 * @param variables The variables.
	 */
	public ScriptExecutingMessageProcessor(ScriptSource scriptSource, ScriptExecutor scriptExecutor,
			Map<String, Object> variables) {

		this(scriptSource, new DefaultScriptVariableGenerator(variables), scriptExecutor);
	}

	@Override
	protected ScriptSource getScriptSource(Message<?> message) {
		return this.scriptSource;
	}

	@Override
	protected Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) {
		Assert.notNull(scriptSource, "scriptSource must not be null");
		return this.scriptExecutor.executeScript(scriptSource, variables);
	}

}
