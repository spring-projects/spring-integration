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

import org.springframework.integration.scripting.ScriptExecutor;

/**
 * @author David Turanski
 * @since 2.1
 */
public abstract class ScriptExecutorFactory {

	public static ScriptExecutor getScriptExecutor(String language) {
		if (language.equalsIgnoreCase("python") || language.equalsIgnoreCase("jython")) {
			return new PythonScriptExecutor();
		}
		else if (language.equalsIgnoreCase("ruby") ||  language.equalsIgnoreCase("jruby")) {
			return new RubyScriptExecutor();
		}
		return new DefaultScriptExecutor(language);
	}

}
