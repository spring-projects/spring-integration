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

package org.springframework.integration.aggregator;

import java.lang.reflect.Method;

import org.springframework.integration.store.MessageGroup;
import org.springframework.util.Assert;

/**
 * Adapter for methods annotated with
 * {@link org.springframework.integration.annotation.ReleaseStrategy @ReleaseStrategy}
 * and for '<code>release-strategy</code>' elements that include a '<code>method</code>'
 * attribute (e.g. &lt;release-strategy ref="beanReference" method="methodName"/&gt;).
 * 
 * @author Marius Bogoevici
 */
public class ReleaseStrategyAdapter extends MessageListMethodAdapter implements ReleaseStrategy {

	public ReleaseStrategyAdapter(Object object, Method method) {
		super(object, method);
		this.assertMethodReturnsBoolean();
	}

	public ReleaseStrategyAdapter(Object object, String methodName) {
		super(object, methodName);
		this.assertMethodReturnsBoolean();
	}


	public boolean canRelease(MessageGroup messages) {
		return ((Boolean) executeMethod(messages.getUnmarked())).booleanValue() && messages.getMarked().isEmpty();
	}
	
	private void assertMethodReturnsBoolean() {
		Assert.isTrue(Boolean.class.equals(this.getMethod().getReturnType())
				|| boolean.class.equals(this.getMethod().getReturnType()),
				"Method '" + getMethod().getName() + "' does not return a boolean value");
	}

}
