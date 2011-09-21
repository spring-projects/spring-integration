/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.scripting;

import java.util.Map;

import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * Base {@link MessageProcessor} for scripting implementations to extend.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractScriptExecutingMessageProcessor<T> implements MessageProcessor<T> {

	private final ScriptVariableGenerator scriptVariableGenerator;


	protected AbstractScriptExecutingMessageProcessor() {
		this.scriptVariableGenerator = new DefaultScriptVariableGenerator();
	}

	protected AbstractScriptExecutingMessageProcessor(ScriptVariableGenerator scriptVariableGenerator) {
		Assert.notNull(scriptVariableGenerator, "scriptVariableGenerator must not be null");
		this.scriptVariableGenerator = scriptVariableGenerator;
	}
	

	/**
	 * Executes the script and returns the result.
	 */
	public final T processMessage(Message<?> message) {
		try {
			ScriptSource source = this.getScriptSource(message);
			Map<String, Object> variables = this.scriptVariableGenerator.generateScriptVariables(message);
			return this.executeScript(source, variables);
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "failed to execute script", e);
		}
	}


	/**
	 * Subclasses must implement this method to create a script source,
	 * optionally using the message to locate or create the script.
	 * 
	 * @param message the message being processed
	 * @return a ScriptSource to use to create a script
	 */
	protected abstract ScriptSource getScriptSource(Message<?> message);

	/**
	 * Subclasses must implement this method. In doing so, the execution context
	 * for the script should be populated with the provided script variables.
	 */
	protected abstract T executeScript(ScriptSource scriptSource, Map<String, Object> variables) throws Exception;

}
