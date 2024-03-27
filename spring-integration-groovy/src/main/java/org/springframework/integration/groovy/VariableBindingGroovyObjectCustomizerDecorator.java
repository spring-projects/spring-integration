/*
 * Copyright 2002-2024 the original author or authors.
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
