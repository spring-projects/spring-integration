/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.router;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.Message;

/**
 * Aggregator adapter for methods annotated with
 * {@link org.springframework.integration.annotation.CompletionStrategy @CompletionStrategy}
 * and for '<code>aggregator</code>' elements that include a '<code>method</code>'
 * attribute (e.g. &lt;aggregator ref="beanReference" method="methodName"/&gt;).
 * 
 * @author Marius Bogoevici
 */

public class CompletionStrategyAdapter extends MessageListMethodAdapter implements CompletionStrategy {

	public CompletionStrategyAdapter(Object object, Method method) {
		super(object, method);
		assertMethodReturnsBoolean();
	}

	public CompletionStrategyAdapter(Object object, String methodName) {
		super(object, methodName);
		assertMethodReturnsBoolean();
	}

	private void assertMethodReturnsBoolean() {
		if (!Boolean.class.equals(this.getMethod().getReturnType())
				&& !boolean.class.equals(this.getMethod().getReturnType())) {
			throw new ConfigurationException("Method " + getMethod().getName()
					+ " does not return a boolean value");
		}
	}

	public boolean isComplete(List<Message<?>> messages) {
		return ((Boolean) executeMethod(messages)).booleanValue();
	}

}
