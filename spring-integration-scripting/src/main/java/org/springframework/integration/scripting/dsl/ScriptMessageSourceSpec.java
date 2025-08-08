/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.scripting.dsl;

import java.util.Collections;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.endpoint.MessageProcessorMessageSource;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.support.MapBuilder;

/**
 * The {@link MessageSourceSpec} for Dynamic Language Scripts.
 * Delegates configuration options to the {@link ScriptSpec}.
 * Produces {@link MessageProcessorMessageSource}.
 * *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see ScriptSpec
 * @see MessageProcessorMessageSource
 */
public class ScriptMessageSourceSpec extends MessageSourceSpec<ScriptMessageSourceSpec, MessageSource<?>>
		implements ComponentsRegistration {

	private final ScriptSpec delegate;

	public ScriptMessageSourceSpec(Resource scriptResource) {
		this.delegate = new ScriptSpec(scriptResource);
	}

	public ScriptMessageSourceSpec(String scriptLocation) {
		this.delegate = new ScriptSpec(scriptLocation);
	}

	/**
	 * The script lang (Groovy, ruby, python etc.).
	 * @param lang the script lang
	 * @return the current spec
	 * @see ScriptSpec#lang
	 */
	public ScriptMessageSourceSpec lang(String lang) {
		this.delegate.lang(lang);
		return this;
	}

	/**
	 * The {@link ScriptVariableGenerator} to use.
	 * @param variableGenerator the {@link ScriptVariableGenerator}
	 * @return the current spec
	 * @see ScriptSpec#variableGenerator(ScriptVariableGenerator)
	 */
	public ScriptMessageSourceSpec variableGenerator(ScriptVariableGenerator variableGenerator) {
		this.delegate.variableGenerator(variableGenerator);
		return this;
	}

	/**
	 * The script variables to use.
	 * @param variables the script variables
	 * @return the current spec
	 * @see ScriptSpec#variables(MapBuilder)
	 */
	public ScriptMessageSourceSpec variables(MapBuilder<?, String, Object> variables) {
		this.delegate.variables(variables);
		return this;
	}

	/**
	 * The script variables to use.
	 * @param variables the script variables
	 * @return the current spec
	 * @see ScriptSpec#variables(Map)
	 */
	public ScriptMessageSourceSpec variables(Map<String, Object> variables) {
		this.delegate.variables(variables);
		return this;
	}

	/**
	 * The script variable to use.
	 * @param name the name of variable
	 * @param value the value of variable
	 * @return the current spec
	 * @see ScriptSpec#variable
	 */
	public ScriptMessageSourceSpec variable(String name, Object value) {
		this.delegate.variable(name, value);
		return this;
	}

	/**
	 * The refreshCheckDelay in milliseconds for refreshable script resource.
	 * @param refreshCheckDelay the refresh check delay milliseconds
	 * @return the current spec
	 * @see ScriptSpec#refreshCheckDelay
	 */
	public ScriptMessageSourceSpec refreshCheckDelay(long refreshCheckDelay) {
		this.delegate.refreshCheckDelay(refreshCheckDelay);
		return this;
	}

	@Override
	protected MessageSource<?> doGet() {
		return new MessageProcessorMessageSource(this.delegate.getObject());
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return Collections.singletonMap(this.delegate.getObject(), this.delegate.getId());
	}

}
