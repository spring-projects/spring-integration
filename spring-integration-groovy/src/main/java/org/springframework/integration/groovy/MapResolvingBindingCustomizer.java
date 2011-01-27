/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import groovy.lang.Script;

import java.util.Map;

import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class MapResolvingBindingCustomizer implements GroovyObjectCustomizer {

	private volatile Map<String, ?> resolvedScriptVariables;


	public void setResolvedScriptVariables(Map<String, ?> resolvedScriptVariables) {
		this.resolvedScriptVariables = resolvedScriptVariables;
	}

	public void customize(GroovyObject goo) {
		Assert.state(goo instanceof Script, "Expected a Script");
		if (this.resolvedScriptVariables != null) {
			Binding binding = ((Script) goo).getBinding();
			for (String key : this.resolvedScriptVariables.keySet()) {
				binding.setVariable(key, this.resolvedScriptVariables.get(key));
			}
		}
	}
}

