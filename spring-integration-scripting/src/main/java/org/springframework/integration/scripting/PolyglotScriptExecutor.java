/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.scripting;

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

	private Context.Builder contextBuilder;

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
			return context.eval(this.language, scriptSource.getScriptAsString()).as(Object.class);
		}
		catch (Exception ex) {
			throw new ScriptingException(ex.getMessage(), ex);
		}
	}

}
