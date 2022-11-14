/*
 * Copyright 2002-2022 the original author or authors.
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
