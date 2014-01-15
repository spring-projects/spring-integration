/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.groovy;

import groovy.lang.Binding;
import groovy.lang.GString;

import java.util.Map;

import org.springframework.integration.scripting.AbstractScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.DefaultScriptVariableGenerator;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.messaging.Message;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.scripting.groovy.GroovyScriptFactory;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Stefan Reuter
 * @since 2.0
 */
public class GroovyCommandMessageProcessor extends AbstractScriptExecutingMessageProcessor<Object> {

	private volatile GroovyObjectCustomizer customizer;

	private Binding binding;


	/**
	 * Creates a {@link GroovyCommandMessageProcessor} that will use the {@link DefaultScriptVariableGenerator}.
	 */
	public GroovyCommandMessageProcessor() {
		super();
	}

	/**
	 * Creates a {@link GroovyCommandMessageProcessor} that will use the provided {@link ScriptVariableGenerator}.
	 *
	 * @param scriptVariableGenerator The variable generator.
	 */
	public GroovyCommandMessageProcessor(ScriptVariableGenerator scriptVariableGenerator) {
		super(scriptVariableGenerator);
	}

	/**
	 * Creates a {@link GroovyCommandMessageProcessor} that will use the {@link DefaultScriptVariableGenerator}
	 * and provided {@link Binding}.
	 * Provided 'binding' will be used in the {@link BindingOverwriteGroovyObjectCustomizerDecorator} to overwrite
	 * original Groovy Script 'binding'.
	 *
	 * @param binding The binding.
	 */
	public GroovyCommandMessageProcessor(Binding binding) {
		this();
		Assert.notNull(binding, "binding must not be null");
		this.binding = binding;
	}

	/**
	 * Creates a {@link GroovyCommandMessageProcessor} that will use the provided {@link ScriptVariableGenerator} and Binding.
	 * Provided 'binding' will be used in the {@link BindingOverwriteGroovyObjectCustomizerDecorator} to overwrite
	 * original Groovy Script 'binding'.
	 *
	 * @param binding The binding.
	 * @param scriptVariableGenerator The variable generator.
	 */
	public GroovyCommandMessageProcessor(Binding binding, ScriptVariableGenerator scriptVariableGenerator) {
		this(scriptVariableGenerator);
		Assert.notNull(binding, "binding must not be null");
		this.binding = binding;
	}

	/**
	 * Sets a {@link GroovyObjectCustomizer} for this processor.
	 *
	 * @param customizer The customizer.
	 */
	public void setCustomizer(GroovyObjectCustomizer customizer) {
		this.customizer = customizer;
	}

	@Override
	protected ScriptSource getScriptSource(Message<?> message) {
		Object payload = message.getPayload();
		Assert.isInstanceOf(String.class, payload, "Payload must be a String containing a Groovy script.");
		String className = generateScriptName(message);
		return new StaticScriptSource((String) payload, className);
	}

	@Override
	protected Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) throws Exception {
		Assert.notNull(scriptSource, "scriptSource must not be null");
		VariableBindingGroovyObjectCustomizerDecorator customizerDecorator = this.binding != null
				? new BindingOverwriteGroovyObjectCustomizerDecorator(this.binding)
				: new VariableBindingGroovyObjectCustomizerDecorator();
		if (this.customizer != null) {
			customizerDecorator.setCustomizer(this.customizer);
		}
		if (!CollectionUtils.isEmpty(variables)) {
			customizerDecorator.setVariables(variables);
		}
		GroovyScriptFactory factory = new GroovyScriptFactory(this.getClass().getSimpleName(), customizerDecorator);
		if (this.beanClassLoader != null) {
			factory.setBeanClassLoader(this.beanClassLoader);
		}
		if (this.beanFactory != null) {
			factory.setBeanFactory(this.beanFactory);
		}
		Object result = factory.getScriptedObject(scriptSource);
		return (result instanceof GString) ? result.toString() : result;
	}

	protected String generateScriptName(Message<?> message) {
		// Don't use the same script (class) name for all invocations by default
		return getClass().getSimpleName() + message.getHeaders().getId().toString().replaceAll("-", "");
	}

}
