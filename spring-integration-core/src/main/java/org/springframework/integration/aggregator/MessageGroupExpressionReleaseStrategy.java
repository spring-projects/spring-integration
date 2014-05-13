/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.expression.Expression;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.util.AbstractExpressionEvaluator;


/**
 * @author Artem Bilan
 * @since 4.0
 */
public class MessageGroupExpressionReleaseStrategy extends AbstractExpressionEvaluator implements ReleaseStrategy {

	private final Expression expression;

	public MessageGroupExpressionReleaseStrategy(Expression expression) {
		this.expression = expression;
	}

	@Override
	public boolean canRelease(MessageGroup group) {
		return evaluateExpression(this.expression, group, Boolean.class);
	}

}
