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

package org.springframework.integration.scripting;

import java.util.Map;

import org.springframework.scripting.ScriptSource;

/**
 * @author David Turanski
 * @author Artem Bilan
 * @since 2.1
 */
@FunctionalInterface
public interface ScriptExecutor {

	/**
	 * @param scriptSource The script source.
	 * @return The result of the execution.
	 */
	default Object executeScript(ScriptSource scriptSource) {
		return executeScript(scriptSource, null);
	}

	/**
	 * @param scriptSource The script source.
	 * @param variables The variables.
	 * @return The result of the execution.
	 */
	Object executeScript(ScriptSource scriptSource, Map<String, Object> variables);

}
