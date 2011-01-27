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

import org.springframework.integration.Message;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class GroovyCommandMessageProcessor extends GroovyScriptExecutingMessageProcessor {
	
	public GroovyCommandMessageProcessor() {
		super(null);
	}
	
	public GroovyCommandMessageProcessor(ScriptVariableSource scriptVariableSource) {
		super(null, scriptVariableSource);
	}

	@Override
	protected ScriptSource getScriptSource(Message<?> message) {
		Object payload = message.getPayload();
		Assert.isInstanceOf(String.class, payload, "Payload must be a String containing a Groovy script.");
		String className = generateScriptName(message);
		return new StaticScriptSource((String) payload, className);
	}

	protected String generateScriptName(Message<?> message) {
		// Don't use the same script (class) name for all invocations by default
		return getClass().getSimpleName() + message.getHeaders().getId().toString().replaceAll("-", "");
	}
}
