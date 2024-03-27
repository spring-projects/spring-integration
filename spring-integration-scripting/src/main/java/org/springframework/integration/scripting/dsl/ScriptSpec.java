/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.scripting.dsl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.integration.dsl.MessageProcessorSpec;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.scripting.DefaultScriptVariableGenerator;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.support.MapBuilder;
import org.springframework.util.Assert;

/**
 * The {@link MessageProcessorSpec} implementation for the {@link DslScriptExecutingMessageProcessor}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ScriptSpec extends MessageProcessorSpec<ScriptSpec> {

	private final DslScriptExecutingMessageProcessor processor;

	private ScriptVariableGenerator variableGenerator;

	private final Map<String, Object> variables = new HashMap<>();

	ScriptSpec(Resource scriptResource) {
		Assert.notNull(scriptResource, "'scriptResource' must not be null");
		this.processor = new DslScriptExecutingMessageProcessor(scriptResource);
	}

	ScriptSpec(String scriptLocation) {
		Assert.hasText(scriptLocation, "'scriptLocation' must not be empty");
		this.processor = new DslScriptExecutingMessageProcessor(scriptLocation);
	}

	/**
	 * The script lang (Groovy, ruby, python etc.).
	 * @param lang the script lang
	 * @return the current spec
	 * @see DslScriptExecutingMessageProcessor#setLang
	 */
	public ScriptSpec lang(String lang) {
		Assert.hasText(lang, "'lang' must not be empty");
		this.processor.setLang(lang);
		return this;
	}

	/**
	 * The refreshCheckDelay in milliseconds for refreshable script resource.
	 * @param refreshCheckDelay the refresh check delay milliseconds
	 * @return the current spec
	 * @see org.springframework.integration.scripting.RefreshableResourceScriptSource
	 */
	public ScriptSpec refreshCheckDelay(long refreshCheckDelay) {
		this.processor.setRefreshCheckDelay(refreshCheckDelay);
		return this;
	}

	/**
	 * The {@link ScriptVariableGenerator} to use.
	 * @param variableGenerator the {@link ScriptVariableGenerator}
	 * @return the current spec
	 * @see org.springframework.integration.scripting.AbstractScriptExecutingMessageProcessor
	 */
	public ScriptSpec variableGenerator(ScriptVariableGenerator variableGenerator) {
		Assert.notNull(variableGenerator, "'variableGenerator' must not be null");
		Assert.state(this.variables.isEmpty(), "'variableGenerator' and 'variables' are mutually exclusive");
		this.variableGenerator = variableGenerator;
		return this;
	}

	/**
	 * The script variables to use.
	 * @param variables the script variables {@link MapBuilder}
	 * @return the current spec
	 * @see DefaultScriptVariableGenerator
	 */
	public ScriptSpec variables(MapBuilder<?, String, Object> variables) {
		return variables(variables.get());
	}

	/**
	 * The script variables to use
	 * @param variables the script variables {@link Map}
	 * @return the current spec
	 * @see DefaultScriptVariableGenerator
	 */
	public ScriptSpec variables(Map<String, Object> variables) {
		Assert.notEmpty(variables, "'variables' must not be empty");
		Assert.state(this.variableGenerator == null, "'variableGenerator' and 'variables' are mutually exclusive");
		this.variables.putAll(variables);
		return this;
	}

	/**
	 * The script variable to use.
	 * @param name the name of variable
	 * @param value the value of variable
	 * @return the current spec
	 * @see DefaultScriptVariableGenerator
	 */
	public ScriptSpec variable(String name, Object value) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.state(this.variableGenerator == null, "'variableGenerator' and 'variables' are mutually exclusive");
		this.variables.put(name, value);
		return this;
	}

	@Override
	protected MessageProcessor<?> doGet() {
		if (this.variableGenerator == null) {
			this.variableGenerator = new DefaultScriptVariableGenerator(this.variables);
		}
		this.processor.setVariableGenerator(this.variableGenerator);
		return this.processor;
	}

}
