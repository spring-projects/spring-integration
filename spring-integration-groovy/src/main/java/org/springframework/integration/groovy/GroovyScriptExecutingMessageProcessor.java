/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.groovy;

import groovy.lang.GString;

import java.util.Map;

import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractScriptExecutingMessageProcessor;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.groovy.GroovyScriptFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class GroovyScriptExecutingMessageProcessor extends AbstractScriptExecutingMessageProcessor<Object> {

	private final GroovyScriptFactory scriptFactory;

	private final VariableBindingGroovyObjectCustomizer customizer = new VariableBindingGroovyObjectCustomizer();

	private volatile ScriptSource scriptSource;

	private final ScriptVariableGenerator scriptVariableGenerator;


	/**
	 * Create a processor for the given {@link ScriptSource}.
	 */
	public GroovyScriptExecutingMessageProcessor(ScriptSource scriptSource) {
		this(scriptSource, new DefaultScriptVariableGenerator());
	}

	public GroovyScriptExecutingMessageProcessor(ScriptSource scriptSource, ScriptVariableGenerator scriptVariableGenerator) {
		this.scriptSource = scriptSource;
		this.scriptVariableGenerator = (scriptVariableGenerator != null) ? scriptVariableGenerator
				: new DefaultScriptVariableGenerator();
		this.scriptFactory = new GroovyScriptFactory(this.getClass().getSimpleName(), this.customizer);
	}


	@Override
	protected ScriptSource getScriptSource(Message<?> message) {
		return this.scriptSource;
	}

	@Override
	protected Object executeScript(ScriptSource scriptSource, Message<?> message) throws Exception {
		Assert.notNull(scriptSource, "scriptSource must not be null");
		Map<String, Object> scriptVariables = this.scriptVariableGenerator.generateScriptVariables(message);
		synchronized (this) {
			if (!CollectionUtils.isEmpty(scriptVariables)) {
				this.customizer.setVariables(scriptVariables);
			}
			Object result = this.scriptFactory.getScriptedObject(scriptSource, null);
			return (result instanceof GString) ? result.toString() : result;
		}
	}

}
