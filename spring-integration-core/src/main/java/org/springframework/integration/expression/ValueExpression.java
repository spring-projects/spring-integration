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

package org.springframework.integration.expression;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.util.Assert;

/**
 * A very simple hardcoded implementation of the Expression interface that represents an
 * Object value. It is used as value holder in cases of context of expression evaluation.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class ValueExpression implements Expression {

	/** Fixed value of this expression */
	private final Object value;

	public ValueExpression(Object value) {
		Assert.notNull(value);
		this.value = value;
	}

	@Override
	public Object getValue() throws EvaluationException {
		return this.value;
	}

	@Override
	public Object getValue(Object rootObject) throws EvaluationException {
		return this.value;
	}

	@Override
	public <T> T getValue(Class<T> desiredResultType) throws EvaluationException {
		TypedValue typedResultValue = new TypedValue(this.getValue(), getValueTypeDescriptor());
		return org.springframework.expression.common.ExpressionUtils
				.convertTypedValue(null, typedResultValue, desiredResultType);
	}

	@Override
	public <T> T getValue(Object rootObject, Class<T> desiredResultType) throws EvaluationException {
		TypedValue typedResultValue = new TypedValue(this.getValue(), getValueTypeDescriptor());
		return org.springframework.expression.common.ExpressionUtils.convertTypedValue(null, typedResultValue, desiredResultType);
	}

	@Override
	public Object getValue(EvaluationContext context) throws EvaluationException {
		return this.value;
	}

	@Override
	public Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
		return this.value;
	}

	@Override
	public <T> T getValue(EvaluationContext context, Class<T> desiredResultType) throws EvaluationException {
		TypedValue typedResultValue = new TypedValue(this.getValue(), getValueTypeDescriptor());
		return org.springframework.expression.common.ExpressionUtils.convertTypedValue(context, typedResultValue, desiredResultType);
	}

	@Override
	public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType) throws EvaluationException {
		TypedValue typedResultValue = new TypedValue(this.getValue(), getValueTypeDescriptor());
		return org.springframework.expression.common.ExpressionUtils.convertTypedValue(context, typedResultValue, desiredResultType);
	}

	@Override
	public Class<?> getValueType() throws EvaluationException {
		return this.value.getClass();
	}

	@Override
	public Class<?> getValueType(Object rootObject) throws EvaluationException {
		return this.value.getClass();
	}

	@Override
	public Class<?> getValueType(EvaluationContext context) throws EvaluationException {
		return null;
	}

	@Override
	public Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException {
		return this.value.getClass();
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
		return TypeDescriptor.valueOf(this.value.getClass());
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException {
		return TypeDescriptor.valueOf(this.value.getClass());
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
		return TypeDescriptor.valueOf(this.value.getClass());
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException {
		return TypeDescriptor.valueOf(this.value.getClass());
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
	public void setValue(EvaluationContext context, Object value) throws EvaluationException {

	}

	@Override
	public void setValue(Object rootObject, Object value) throws EvaluationException {
		throw new EvaluationException(this.value.toString(), "Cannot call setValue() on a ValueExpression");
	}

	@Override
	public void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException {
		throw new EvaluationException(this.value.toString(), "Cannot call setValue() on a ValueExpression");
	}

	@Override
	public String getExpressionString() {
		return this.value.toString();
	}

}
