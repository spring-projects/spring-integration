/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHandlingException;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * Base {@link MessageProcessor} for scripting implementations to extend.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractScriptExecutingMessageProcessor implements MessageProcessor {

	private final ScriptSource scriptSource;


	/**
	 * Create a processor for the given {@link ScriptSource}.
	 */
	public AbstractScriptExecutingMessageProcessor(ScriptSource scriptSource) {
		Assert.notNull(scriptSource, "scriptSource must not be null");
		this.scriptSource = scriptSource;
	}


	/**
	 * Executes the script and returns the result.
	 */
	public final Object processMessage(Message<?> message) {
		try {
			return this.executeScript(this.scriptSource, message);
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "failed to execute script", e);
		}
	}

	/**
	 * Subclasses must implement this method. In doing so, the execution context for the
	 * script should be populated with the Message's 'payload' and 'headers' as variables. 
	 */
	protected abstract Object executeScript(ScriptSource scriptSource, Message<?> message) throws Exception;

}
