/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
