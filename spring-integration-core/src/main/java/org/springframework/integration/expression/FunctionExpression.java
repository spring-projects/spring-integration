/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.expression;

import java.util.function.Function;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * An {@link Expression} that simply invokes {@link Function#apply(Object)} on its
 * provided {@link Function}.
 * <p>
 * This is a powerful alternative to the SpEL, when Java 8 and its Lambda support is in use.
 * <p>
 * If the target component has support for an {@link Expression} property,
 * a {@link FunctionExpression} can be specified instead of a
 * {@link org.springframework.expression.spel.standard.SpelExpression}
 * as an alternative to evaluate the value from the Lambda, rather than runtime SpEL resolution.
 * <p>
 * The {@link FunctionExpression} is 'read-only', hence only {@link #getValue} operations
 * are allowed.
 * Any {@link #setValue} operations and {@link #getValueType} related operations
 * throw {@link EvaluationException}.
 *
 * @param <S> The evaluation context root object type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 5.0
 */
public class FunctionExpression<S> implements Expression {

	private final Function<S, ?> function;

	private final EvaluationContext defaultContext = new StandardEvaluationContext();

	private final EvaluationException readOnlyException;

	public FunctionExpression(Function<S, ?> function) {
		Assert.notNull(function, "'function' must not be null.");
		this.function = function;
		this.readOnlyException = new EvaluationException(getExpressionString(),
				"FunctionExpression is a 'read only' Expression implementation");
	}

	@Override
	public Object getValue() throws EvaluationException {
		return this.function.apply(null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getValue(Object rootObject) throws EvaluationException {
		return this.function.apply((S) rootObject);
	}

	@Override
	public <T> T getValue(Class<T> desiredResultType) throws EvaluationException {
		return getValue(this.defaultContext, desiredResultType);
	}

	@Override
	public <T> T getValue(Object rootObject, Class<T> desiredResultType) throws EvaluationException {
		return getValue(this.defaultContext, rootObject, desiredResultType);
	}

	@Override
	public Object getValue(EvaluationContext context) throws EvaluationException {
		return getValue(context.getRootObject().getValue());
	}

	@Override
	public Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
		return getValue(rootObject);
	}

	@Override
	public <T> T getValue(EvaluationContext context, Class<T> desiredResultType) throws EvaluationException {
		return ExpressionUtils.convertTypedValue(context, new TypedValue(getValue(context)), desiredResultType);
	}

	@Override
	public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType)
			throws EvaluationException {
		return ExpressionUtils.convertTypedValue(context, new TypedValue(getValue(rootObject)), desiredResultType);
	}

	@Override
	public Class<?> getValueType() throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public Class<?> getValueType(Object rootObject) throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public Class<?> getValueType(EvaluationContext context) throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject)
			throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public void setValue(EvaluationContext context, Object value) throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public void setValue(Object rootObject, Object value) throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException {
		throw this.readOnlyException;
	}

	@Override
	public boolean isWritable(EvaluationContext context) throws EvaluationException {
		return false;
	}

	@Override
	public boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException {
		return false;
	}

	@Override
	public boolean isWritable(Object rootObject) throws EvaluationException {
		return false;
	}

	@Override
	public String getExpressionString() {
		return this.function.toString();
	}

}
