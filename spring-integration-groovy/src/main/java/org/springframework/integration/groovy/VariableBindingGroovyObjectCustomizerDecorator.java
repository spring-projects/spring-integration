/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy;

import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import groovy.lang.Script;

import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
class VariableBindingGroovyObjectCustomizerDecorator implements GroovyObjectCustomizer {

	private volatile Map<String, ?> variables;

	private volatile GroovyObjectCustomizer customizer;

	public void setVariables(Map<String, ?> variables) {
		this.variables = variables;
	}

	public void setCustomizer(GroovyObjectCustomizer customizer) {
		this.customizer = customizer;
	}

	public void customize(GroovyObject goo) {
		Assert.state(goo instanceof Script, "Expected a Script");
		if (this.variables != null) {
			Binding binding = ((Script) goo).getBinding();
			for (Map.Entry<String, ?> entry : this.variables.entrySet()) {
				binding.setVariable(entry.getKey(), entry.getValue());
			}
		}
		if (this.customizer != null) {
			this.customizer.customize(goo);
		}
	}

}
