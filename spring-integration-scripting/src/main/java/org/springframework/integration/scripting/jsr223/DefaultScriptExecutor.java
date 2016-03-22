/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.scripting.jsr223;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.springframework.integration.scripting.ScriptExecutor;

/**
 * Default implementation of the {@link ScriptExecutor}
 *
 * @author David Turanski
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
public class DefaultScriptExecutor extends AbstractScriptExecutor {

	/**
	 * Create a DefaultScriptExecutor for the specified language name (JSR233 alias).
	 * @param language the scripting language identificator.
	 */
	public DefaultScriptExecutor(String language) {
		super(language);
	}

	@Override
	protected Object postProcess(Object result, ScriptEngine scriptEngine, String script, Bindings bindings) {
		return result;
	}

}
