/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import groovy.lang.Script;

import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 * @since 2.2
 */

class BindingOverwriteGroovyObjectCustomizerDecorator extends VariableBindingGroovyObjectCustomizerDecorator {

	private final Binding binding;

	BindingOverwriteGroovyObjectCustomizerDecorator(Binding binding) {
		Assert.notNull(binding, "binding must not be null");
		this.binding = binding;
	}

	@Override
	public void customize(GroovyObject goo) {
		Assert.state(goo instanceof Script, "Expected a Script");
		((Script) goo).setBinding(this.binding);
		super.customize(goo);
	}

}
