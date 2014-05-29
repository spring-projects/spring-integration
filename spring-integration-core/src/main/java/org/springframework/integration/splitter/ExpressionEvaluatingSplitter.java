/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.splitter;

import java.util.List;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;

/**
 * A Message Splitter implementation that evaluates the specified SpEL
 * expression. The result of evaluation will typically be a Collection or
 * Array. If the result is not a Collection or Array, then the single Object
 * will be returned as the payload of a single reply Message.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class ExpressionEvaluatingSplitter extends AbstractMessageProcessingSplitter {

	@SuppressWarnings({"unchecked", "rawtypes"})
	public ExpressionEvaluatingSplitter(Expression expression) {
		super(new ExpressionEvaluatingMessageProcessor(expression, List.class));
	}

}
