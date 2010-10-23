/*
 * Copyright 2002-2010 the original author or authors.
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

import groovy.lang.GString;

import java.util.Map;

import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractScriptExecutingMessageProcessor;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.groovy.GroovyScriptFactory;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @since 2.0
 */
public class GroovyScriptPayloadMessageProcessor extends AbstractScriptExecutingMessageProcessor<Object> {

	private final Map<String, ?> map;

	public GroovyScriptPayloadMessageProcessor() {
		this(null);
	}

	public GroovyScriptPayloadMessageProcessor(Map<String, ?> map) {
		this.map = map;
	}

	@Override
	protected ScriptSource getScriptSource(Message<?> message) {
		Object payload = message.getPayload();
		Assert.isInstanceOf(String.class, payload, "Payload must be String containing Groovy script.");
		String className = generateScriptName(message);
		return new StaticScriptSource((String) payload, className);
	}

	@Override
	protected Object executeScript(ScriptSource scriptSource, Message<?> message) throws Exception {
		// Keeping everything local prevents PermGen (class instances) leaks...
		MessageContextBindingCustomizer bindingCustomizer = new MessageContextBindingCustomizer(this.map);
		bindingCustomizer.setMessage(message);
		GroovyScriptFactory scriptFactory = new GroovyScriptFactory(this.getClass().getSimpleName(), bindingCustomizer);
		Object result = scriptFactory.getScriptedObject(scriptSource, null);
		return (result instanceof GString) ? result.toString() : result;
	}
	
	protected String generateScriptName(Message<?> message) {
		// Don't use the same script (class) name for all invocations by default
		return getClass().getSimpleName() + message.getHeaders().getId().toString().replaceAll("-", "");
	}

}
