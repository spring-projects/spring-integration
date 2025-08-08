/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;

import groovy.lang.Binding;
import groovy.lang.GString;

import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.scripting.AbstractScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.scripting.groovy.GroovyScriptFactory;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Stefan Reuter
 * @author Gary Russell
 *
 * @since 2.0
 */
public class GroovyCommandMessageProcessor extends AbstractScriptExecutingMessageProcessor<Object>
		implements IntegrationPattern {

	private GroovyObjectCustomizer customizer;

	@Nullable
	private Binding binding;

	/**
	 * Creates a {@link GroovyCommandMessageProcessor} that will use the
	 * {@link org.springframework.integration.scripting.DefaultScriptVariableGenerator}.
	 */
	public GroovyCommandMessageProcessor() {
	}

	/**
	 * Creates a {@link GroovyCommandMessageProcessor} that will use the provided {@link ScriptVariableGenerator}.
	 * @param scriptVariableGenerator The variable generator.
	 */
	public GroovyCommandMessageProcessor(ScriptVariableGenerator scriptVariableGenerator) {
		super(scriptVariableGenerator);
	}

	/**
	 * Creates a {@link GroovyCommandMessageProcessor} that will use the
	 * {@link org.springframework.integration.scripting.DefaultScriptVariableGenerator}
	 * and provided {@link Binding}.
	 * Provided 'binding' will be used in the {@link BindingOverwriteGroovyObjectCustomizerDecorator} to overwrite
	 * original Groovy Script 'binding'.
	 * @param binding The binding.
	 */
	public GroovyCommandMessageProcessor(Binding binding) {
		Assert.notNull(binding, "binding must not be null");
		this.binding = binding;
	}

	/**
	 * Creates a {@link GroovyCommandMessageProcessor} that will use the provided {@link ScriptVariableGenerator} and
	 * Binding.
	 * Provided 'binding' will be used in the {@link BindingOverwriteGroovyObjectCustomizerDecorator} to overwrite
	 * original Groovy Script 'binding'.
	 * @param binding The binding.
	 * @param scriptVariableGenerator The variable generator.
	 */
	public GroovyCommandMessageProcessor(Binding binding, ScriptVariableGenerator scriptVariableGenerator) {
		this(scriptVariableGenerator);
		Assert.notNull(binding, "binding must not be null");
		this.binding = binding;
	}

	/**
	 * Sets a {@link GroovyObjectCustomizer} for this processor.
	 * @param customizer The customizer.
	 */
	public void setCustomizer(GroovyObjectCustomizer customizer) {
		this.customizer = customizer;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.control_bus;
	}

	@Override
	protected ScriptSource getScriptSource(Message<?> message) {
		Object payload = message.getPayload();
		Assert.isInstanceOf(String.class, payload, "Payload must be a String containing a Groovy script.");
		String className = generateScriptName(message);
		return new StaticScriptSource((String) payload, className);
	}

	@Override
	protected Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) {
		Assert.notNull(scriptSource, "scriptSource must not be null");
		VariableBindingGroovyObjectCustomizerDecorator customizerDecorator =
				this.binding != null
						? new BindingOverwriteGroovyObjectCustomizerDecorator(this.binding)
						: new VariableBindingGroovyObjectCustomizerDecorator();
		if (this.customizer != null) {
			customizerDecorator.setCustomizer(this.customizer);
		}
		if (!CollectionUtils.isEmpty(variables)) {
			customizerDecorator.setVariables(variables);
		}
		GroovyScriptFactory factory = new GroovyScriptFactory(getClass().getSimpleName(), customizerDecorator);
		ClassLoader beanClassLoader = getBeanClassLoader();
		if (beanClassLoader != null) {
			factory.setBeanClassLoader(beanClassLoader);
		}
		factory.setBeanFactory(getBeanFactory());
		try {
			Object result = factory.getScriptedObject(scriptSource);
			return (result instanceof GString) ? result.toString() : result;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected String generateScriptName(Message<?> message) {
		// Don't use the same script (class) name for all invocations by default
		UUID id = message.getHeaders().getId();
		return getClass().getSimpleName()
				+ (id != null ? id.toString().replaceAll("-", "") : ObjectUtils.getIdentityHexString(message));
	}

}
