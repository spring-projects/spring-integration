/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.expression;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.util.Assert;

/**
 * A very simple hardcoded implementation of the {@link Expression} interface that represents an
 * immutable value. It is used as value holder in the context of expression evaluation.
 *
 * @param <V> - The expected value type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class ValueExpression<V> implements Expression {

	/**
	 * Fixed value of this expression.
	 */
	private final V value;

	private final Class<V> aClass;

	private final TypedValue typedResultValue;

	private final TypeDescriptor typeDescriptor;

	@SuppressWarnings({"unchecked", "NullAway"})
	public ValueExpression(V value) {
		Assert.notNull(value, "'value' must not be null");
		this.value = value;
		this.aClass = (Class<V>) this.value.getClass();
		this.typedResultValue = new TypedValue(this.value);
		this.typeDescriptor = this.typedResultValue.getTypeDescriptor();
	}

	@Override
	public V getValue() throws EvaluationException {
		return this.value;
	}

	@Override
	public V getValue(@Nullable Object rootObject) throws EvaluationException {
		return this.value;
	}

	@Override
	public V getValue(EvaluationContext context) throws EvaluationException {
		return this.value;
	}

	@Override
	public V getValue(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		return this.value;
	}

	@Override
	public <T> T getValue(@Nullable Object rootObject, @Nullable Class<T> desiredResultType)
			throws EvaluationException {

		return getValue(desiredResultType);
	}

	@Override
	public <T> T getValue(@Nullable Class<T> desiredResultType) throws EvaluationException {
		T aValue = ExpressionUtils.convertTypedValue(null, this.typedResultValue, desiredResultType);
		return Objects.requireNonNull(aValue);
	}

	@Override
	public <T> T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> desiredResultType)
			throws EvaluationException {

		return getValue(context, desiredResultType);
	}

	@Override
	public <T> T getValue(EvaluationContext context, @Nullable Class<T> desiredResultType) throws EvaluationException {
		T aValue = ExpressionUtils.convertTypedValue(context, this.typedResultValue, desiredResultType);
		return Objects.requireNonNull(aValue);
	}

	@Override
	public Class<V> getValueType() throws EvaluationException {
		return this.aClass;
	}

	@Override
	public Class<V> getValueType(@Nullable Object rootObject) throws EvaluationException {
		return this.aClass;
	}

	@Override
	public Class<V> getValueType(EvaluationContext context) throws EvaluationException {
		return this.aClass;
	}

	@Override
	public Class<V> getValueType(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		return this.aClass;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
		return this.typeDescriptor;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(@Nullable Object rootObject) throws EvaluationException {
		return this.typeDescriptor;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
		return this.typeDescriptor;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, @Nullable Object rootObject)
			throws EvaluationException {

		return this.typeDescriptor;
	}

	@Override
	public boolean isWritable(EvaluationContext context) throws EvaluationException {
		return false;
	}

	@Override
	public boolean isWritable(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		return false;
	}

	@Override
	public boolean isWritable(@Nullable Object rootObject) throws EvaluationException {
		return false;
	}

	@Override
	public void setValue(EvaluationContext context, @Nullable Object value) throws EvaluationException {
		setValue(context, null, value);
	}

	@Override
	public void setValue(@Nullable Object rootObject, @Nullable Object value) throws EvaluationException {
		throw new EvaluationException(this.value.toString(), "Cannot call setValue() on a ValueExpression");
	}

	@Override
	public void setValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Object value)
			throws EvaluationException {

		throw new EvaluationException(this.value.toString(), "Cannot call setValue() on a ValueExpression");
	}

	@Override
	public String getExpressionString() {
		return this.value.toString();
	}

	@Override
	public String toString() {
		return "ValueExpression [value=" + this.value + "]";
	}

}
