/*
 * Copyright 2002-2012 the original author or authors.
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
import groovy.lang.MissingPropertyException;
import org.springframework.util.Assert;

/**
 * Bridge {@link Binding} implementation which uses {@link VariableResolver} strategy
 * to resolve Groovy variable as delegate if the last one isn't contained in the original Groovy script {@link Binding}.
 *
 * @author Artem Bilan
 * @since 2.2
 */
public class GroovyVariableResolverBinding extends Binding {

	private VariableResolver variableResolver;

	/**
	 * Create a {@link Binding} for the given {@link VariableResolver} that will be used as
	 * delegate <code>variableResolver</code>.
	 */
	public GroovyVariableResolverBinding(VariableResolver variableResolver) {
		Assert.notNull(variableResolver, "variableResolver must not be null");
		this.variableResolver = variableResolver;
	}

	@Override
	public Object getVariable(String name) {
		try {
			return super.getVariable(name);
		}
		catch (MissingPropertyException e) {
//			Original {@link Binding} doesn't have 'variable' for the given 'name'.
//			Try to resolve it by using <code>variableResolver</code> strategy.
		}
		Object result = this.variableResolver.resolveVariableName(name);
		if (result == null) {
			throw new MissingPropertyException(name, this.getClass());
		}
		return result;
	}
}
