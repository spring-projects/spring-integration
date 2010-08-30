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

package org.springframework.integration.groovy;

import groovy.lang.Binding;
import groovy.lang.GString;
import groovy.lang.GroovyObject;
import groovy.lang.Script;

import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractScriptExecutingMessageProcessor;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.scripting.groovy.GroovyScriptFactory;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @since 2.0
 */
public class GroovyScriptExecutingMessageProcessor extends AbstractScriptExecutingMessageProcessor<Object> {

	private final GroovyScriptFactory scriptFactory;

	private final MessageContextBindingCustomizer customizer = new MessageContextBindingCustomizer();


	public GroovyScriptExecutingMessageProcessor(ScriptSource scriptSource) {
		super(scriptSource);
		this.scriptFactory = new GroovyScriptFactory(this.getClass().getSimpleName(), this.customizer);
	}


	@Override
	protected Object executeScript(ScriptSource scriptSource, Message<?> message) throws Exception {
		synchronized (this) {
			this.customizer.setMessage(message);
			Object result = this.scriptFactory.getScriptedObject(scriptSource, null);
			return (result instanceof GString) ? result.toString() : result;
		}
	}


	private static class MessageContextBindingCustomizer implements GroovyObjectCustomizer {

		private volatile Message<?> message;

		public void setMessage(Message<?> message) {
			this.message = message;
		}

		public void customize(GroovyObject goo) {
			Assert.state(goo instanceof Script, "Expected a Script");
			if (this.message != null) {
				Binding binding = ((Script) goo).getBinding();
				binding.setVariable("payload", this.message.getPayload());
				binding.setVariable("headers", this.message.getHeaders());
			}
		}
	}

}
