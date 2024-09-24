/*
 * Copyright 2022-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import org.springframework.lang.Nullable;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * GraalVM Polyglot {@link ScriptExecutor} implementation.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class PolyglotScriptExecutor implements ScriptExecutor {

	private final String language;

	private final Context.Builder contextBuilder;

	/**
	 * Construct an executor based on the provided language id.
	 * @param language the supported by GraalVM language id.
	 */
	public PolyglotScriptExecutor(String language) {
		this(language, Context.newBuilder().allowAllAccess(true));
	}

	/**
	 * Construct an executor based on the provided language id.
	 * @param language the supported by GraalVM language id.
	 */
	public PolyglotScriptExecutor(String language, Context.Builder contextBuilder) {
		Assert.hasText(language, "'language' must not be empty");
		Assert.notNull(contextBuilder, "'contextBuilder' must not be null");
		this.contextBuilder = contextBuilder;
		this.language = language;
		try (Context context = this.contextBuilder.build()) {
			context.initialize(language);
		}
	}

	@Override
	public Object executeScript(ScriptSource scriptSource, @Nullable Map<String, Object> variables) {
		try (Context context = this.contextBuilder.build()) {
			if (variables != null) {
				Value bindings = context.getBindings(this.language);
				variables.forEach(bindings::putMember);
			}
			String scriptAsString = scriptSource.getScriptAsString();
			Object result = context.eval(this.language, scriptAsString).as(Object.class);
			// We have to copy all the expected PolyglotWrapper instances before context is closed.
			if (result instanceof Map<?, ?> map) {
				String returnVariable = parseReturnVariable(scriptAsString);
				result = map.get(returnVariable);
			}
			if (result instanceof List<?> list) {
				result = new ArrayList<>(list);
			}
			return result;
		}
		catch (Exception ex) {
			throw new ScriptingException(ex.getMessage(), ex);
		}
	}

	private static String parseReturnVariable(String script) {
		String[] lines = script.trim().split("\n");
		String lastLine = lines[lines.length - 1];
		String[] tokens = lastLine.split("=");
		return tokens[0].trim();
	}

}
