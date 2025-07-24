/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.util.Assert;

/**
 * An implementation of {@link Expression} that delegates to an {@link ExpressionSource}
 * for resolving the actual Expression instance per-invocation at runtime.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class DynamicExpression implements Expression {

	private final String key;

	private final ExpressionSource expressionSource;

	public DynamicExpression(String key, ExpressionSource expressionSource) {
		Assert.notNull(key, "key must not be null");
		Assert.notNull(expressionSource, "expressionSource must not be null");
		this.key = key;
		this.expressionSource = expressionSource;
	}

	public @Nullable Object getValue() throws EvaluationException {
		return getValue((Object) null);
	}

	public @Nullable Object getValue(@Nullable Object rootObject) throws EvaluationException {
		return getValue(rootObject, null);
	}

	public <T> @Nullable T getValue(@Nullable Class<T> desiredResultType) throws EvaluationException {
		return getValue((Object) null, desiredResultType);
	}

	public <T> @Nullable T getValue(@Nullable Object rootObject, @Nullable Class<T> desiredResultType)
			throws EvaluationException {

		return resolveExpression().getValue(rootObject, desiredResultType);
	}

	public @Nullable Object getValue(EvaluationContext context) throws EvaluationException {
		return resolveExpression().getValue(context);
	}

	public @Nullable Object getValue(EvaluationContext context, @Nullable  Object rootObject)
			throws EvaluationException {

		return getValue(context, rootObject, null);
	}

	public <T> @Nullable T getValue(EvaluationContext context, @Nullable Class<T> desiredResultType)
			throws EvaluationException {

		return resolveExpression().getValue(context, desiredResultType);
	}

	public <T> @Nullable T getValue(EvaluationContext context, @Nullable Object rootObject,
			@Nullable Class<T> desiredResultType) throws EvaluationException {

		return resolveExpression().getValue(context, rootObject, desiredResultType);
	}

	public @Nullable Class<?> getValueType() throws EvaluationException {
		return getValueType((Object) null);
	}

	public @Nullable Class<?> getValueType(@Nullable Object rootObject) throws EvaluationException {
		return resolveExpression().getValueType(rootObject);
	}

	public @Nullable Class<?> getValueType(EvaluationContext context) throws EvaluationException {
		return resolveExpression().getValueType(context);
	}

	public @Nullable Class<?> getValueType(EvaluationContext context, @Nullable Object rootObject)
			throws EvaluationException {

		return resolveExpression().getValueType(context, rootObject);
	}

	public @Nullable TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
		return getValueTypeDescriptor((Object) null);
	}

	public @Nullable TypeDescriptor getValueTypeDescriptor(@Nullable Object rootObject) throws EvaluationException {
		return this.resolveExpression().getValueTypeDescriptor(rootObject);
	}

	public @Nullable TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
		return resolveExpression().getValueTypeDescriptor(context);
	}

	public @Nullable TypeDescriptor getValueTypeDescriptor(EvaluationContext context, @Nullable Object rootObject)
			throws EvaluationException {

		return resolveExpression().getValueTypeDescriptor(context, rootObject);
	}

	public boolean isWritable(EvaluationContext context) throws EvaluationException {
		return resolveExpression().isWritable(context);
	}

	public boolean isWritable(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		return resolveExpression().isWritable(context, rootObject);
	}

	public boolean isWritable(@Nullable Object rootObject) throws EvaluationException {
		return resolveExpression().isWritable(rootObject);
	}

	public void setValue(EvaluationContext context, @Nullable Object value) throws EvaluationException {
		resolveExpression().setValue(context, value);
	}

	public void setValue(@Nullable Object rootObject, @Nullable Object value) throws EvaluationException {
		resolveExpression().setValue(rootObject, value);
	}

	public void setValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Object value)
			throws EvaluationException {

		resolveExpression().setValue(context, rootObject, value);
	}

	public String getExpressionString() {
		return resolveExpression().getExpressionString();
	}

	private Expression resolveExpression() {
		Locale locale = LocaleContextHolder.getLocale();
		Expression expression = this.expressionSource.getExpression(this.key, locale);
		Assert.state(expression != null, "Unable to resolve Expression with key '" + this.key + "'");
		return expression;
	}

}
