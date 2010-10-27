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

package org.springframework.integration.control;

import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageProcessor;

/**
 * A MessageProcessor implementation that expects an Expression or expressionString
 * as the Message payload. When processing, it simply evaluates that expression.
 * 
 * @author Dave Syer
 * @author Mark Fisher
 * @since 2.0
 */
public class ExpressionPayloadMessageProcessor extends AbstractMessageProcessor<Object> {

	/**
	 * Evaluates the Message payload expression.
	 * @throws IllegalArgumentException if the payload is not an Exception or String
	 */
	public Object processMessage(Message<?> message) {
		Object expression = message.getPayload();
		if (expression instanceof Expression) {
			return evaluateExpression((Expression) expression, message);
		}
		if (expression instanceof String) {
			return evaluateExpression((String) expression, message);
		}
		throw new IllegalArgumentException("Message payload must be an Expression instance or an expression String.");
	}

}
