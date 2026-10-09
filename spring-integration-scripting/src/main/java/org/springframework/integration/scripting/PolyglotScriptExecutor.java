/*
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.scripting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.jspecify.annotations.Nullable;

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
	public @Nullable Object executeScript(ScriptSource scriptSource, @Nullable Map<String, Object> variables) {
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
