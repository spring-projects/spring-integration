/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.integration.store.MessageGroup;

/**
 * A {@link ReleaseStrategy} that evaluates an expression.
 *
 * @author Dave Syer
 */
public class ExpressionEvaluatingReleaseStrategy extends ExpressionEvaluatingMessageListProcessor implements
		ReleaseStrategy {

	public ExpressionEvaluatingReleaseStrategy(String expression) {
		super(expression, Boolean.class);
	}

	/**
	 * Evaluate the expression provided on the messages (a collection) in the group and return the result (must
	 * be boolean).
	 */
	public boolean canRelease(MessageGroup messages) {
		return (Boolean) process(messages.getMessages());
	}

}
