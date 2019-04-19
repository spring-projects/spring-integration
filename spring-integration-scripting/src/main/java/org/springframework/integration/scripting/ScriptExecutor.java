/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.scripting.ScriptSource;

/**
 * A script evaluation abstraction against {@link ScriptSource} and optional binding {@code variables}.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */
@FunctionalInterface
public interface ScriptExecutor {

	/**
	 * Execute a script from the provided {@link ScriptSource} with an optional binding {@code variables}.
	 * @param scriptSource The script source.
	 * @param variables The variables.
	 * @return The result of the execution.
	 */
	@Nullable
	Object executeScript(ScriptSource scriptSource, @Nullable Map<String, Object> variables);

	/**
	 * Execute a script from the provided {@link ScriptSource}
	 * @param scriptSource The script source.
	 * @return The result of the execution.
	 */
	@Nullable
	default Object executeScript(ScriptSource scriptSource) {
		return executeScript(scriptSource, null);
	}

}
