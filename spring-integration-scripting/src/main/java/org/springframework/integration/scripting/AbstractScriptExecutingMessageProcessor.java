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

package org.springframework.integration.scripting;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * Base {@link MessageProcessor} for scripting implementations to extend.
 *
 * @param <T> the paylaod type.
 *
 * @author Mark Fisher
 * @author Stefan Reuter
 * @author Artem Bilan
 *
 * @since 2.0
 */
public abstract class AbstractScriptExecutingMessageProcessor<T>
		implements MessageProcessor<T>, BeanClassLoaderAware, BeanFactoryAware {

	private final ScriptVariableGenerator scriptVariableGenerator;

	private ClassLoader beanClassLoader;

	private BeanFactory beanFactory;

	protected AbstractScriptExecutingMessageProcessor() {
		this(new DefaultScriptVariableGenerator());
	}

	protected AbstractScriptExecutingMessageProcessor(ScriptVariableGenerator scriptVariableGenerator) {
		Assert.notNull(scriptVariableGenerator, "scriptVariableGenerator must not be null");
		this.scriptVariableGenerator = scriptVariableGenerator;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected ScriptVariableGenerator getScriptVariableGenerator() {
		return this.scriptVariableGenerator;
	}

	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Execute the script and return the result.
	 */
	@Override
	@Nullable
	public final T processMessage(Message<?> message) {
		ScriptSource source = getScriptSource(message);
		Map<String, Object> variables = this.scriptVariableGenerator.generateScriptVariables(message);
		return executeScript(source, variables);
	}

	/**
	 * Subclasses must implement this method to create a script source,
	 * optionally using the message to locate or create the script.
	 * @param message the message being processed
	 * @return a ScriptSource to use to create a script
	 */
	protected abstract ScriptSource getScriptSource(Message<?> message);

	/**
	 * Subclasses must implement this method. In doing so, the execution context
	 * for the script should be populated with the provided script variables.
	 * @param scriptSource The script source.
	 * @param variables The variables.
	 * @return The result of the execution.
	 */
	@Nullable
	protected abstract T executeScript(ScriptSource scriptSource, Map<String, Object> variables);

}
