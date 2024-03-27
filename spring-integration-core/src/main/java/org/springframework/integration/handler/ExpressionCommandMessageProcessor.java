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

package org.springframework.integration.handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodFilter;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

/**
 * A MessageProcessor implementation that expects an Expression or expressionString
 * as the Message payload. When processing, it simply evaluates that expression.
 *
 * @author Dave Syer
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ExpressionCommandMessageProcessor extends AbstractMessageProcessor<Object>
		implements IntegrationPattern {

	@Nullable
	private final MethodFilter methodFilter;

	public ExpressionCommandMessageProcessor() {
		this.methodFilter = null;
	}

	public ExpressionCommandMessageProcessor(@Nullable MethodFilter methodFilter) {
		this(methodFilter, null);
	}

	public ExpressionCommandMessageProcessor(@Nullable MethodFilter methodFilter, @Nullable BeanFactory beanFactory) {
		this.methodFilter = methodFilter;
		if (beanFactory != null) {
			setBeanFactory(beanFactory);
		}
	}

	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (this.methodFilter != null) {
			MethodResolver methodResolver = new ExpressionCommandMethodResolver(this.methodFilter);
			getEvaluationContext().setMethodResolvers(Collections.singletonList(methodResolver));
		}
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.control_bus;
	}

	/**
	 * Evaluates the Message payload expression as a command.
	 * @throws IllegalArgumentException if the payload is not an Exception or String
	 */
	@Override
	@Nullable
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

	private static final class ExpressionCommandMethodResolver extends ReflectiveMethodResolver {

		private final MethodFilter methodFilter;

		ExpressionCommandMethodResolver(MethodFilter methodFilter) {
			this.methodFilter = methodFilter;
		}

		@Override
		public MethodExecutor resolve(EvaluationContext context,
				Object targetObject, String name, List<TypeDescriptor> argumentTypes)
				throws AccessException {

			validateMethod(targetObject, name, !CollectionUtils.isEmpty(argumentTypes) ? argumentTypes.size() : 0);
			return super.resolve(context, targetObject, name, argumentTypes);
		}

		private void validateMethod(Object targetObject, String name, int argumentCount) {
			Class<?> type = (targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass());
			Method[] methods = type.getMethods();
			List<Method> candidates = new ArrayList<>();
			for (Method method : methods) {
				if (method.getName().equals(name) && method.getParameterTypes().length == argumentCount) {
					candidates.add(method);
				}
			}
			List<Method> supportedMethods = this.methodFilter.filter(candidates);
			if (supportedMethods.size() == 0) {
				String methodDescription = (candidates.size() > 0) ? candidates.get(0).toString() : name;
				throw new EvaluationException("The method '" + methodDescription +
						"' is not supported by this command processor. " +
						"If using the Control Bus, consider adding @ManagedOperation or @ManagedAttribute.");
			}
		}

	}

}
