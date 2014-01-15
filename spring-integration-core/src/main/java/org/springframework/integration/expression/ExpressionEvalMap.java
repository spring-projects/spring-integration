/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.expression;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.util.Assert;

/**
 * <p>
 * An immutable {@link AbstractMap} implementation that wraps a {@code Map<String, Object>},
 * where values must be instances of {@link String} or {@link Expression},
 * and evaluates an {@code expression} for the provided {@code key} from the underlying
 * {@code original} Map.
 * </p>
 * <p>
 * Any mutating operations ({@link #put(String, Object)}, {@link #remove(Object)} etc.)
 * are not allowed on instances of this class. Mutation can be performed on underlying Map
 * if it supports it.
 * </p>
 * <p>
 * A {@link ExpressionEvalMapBuilder} must be used to instantiate this class
 * via its {@link #from(Map)} method:
 * </p>
 * <pre class="code">
 * {@code
 *ExpressionEvalMap evalMap = ExpressionEvalMap
 *	.from(expressions)
 *	.usingCallback(new EvaluationCallback() {
 *		Object evaluate(Expression expression) {
 *			// return some expression evaluation
 *		}
 *	})
 *	.build();
 *}
 * </pre>
 * <p>
 * Thread-safety depends on the original underlying Map.
 * Objects of this class are not serializable.
 * </p>
 *
 * @author Artem Bilan
 * @since 3.0
 */
public final class ExpressionEvalMap extends AbstractMap<String, Object> {

	public static final EvaluationCallback SIMPLE_CALLBACK = new EvaluationCallback() {

		@Override
		public Object evaluate(Expression expression) {
			return expression.getValue();
		}

	};

	private final Map<String, ?> original;

	private final EvaluationCallback evaluationCallback;

	private ExpressionEvalMap(Map<String, ?> original, EvaluationCallback evaluationCallback) {
		this.original = original;
		this.evaluationCallback = evaluationCallback;
	}

	/**
	 * Gets the {@code value}({@link Expression}) for the provided {@code key}
	 * from {@link #original} and returns the result of evaluation using {@link #evaluationCallback}.
	 */
	@Override
	public Object get(Object key) {
		Object value = original.get(key);
		if (value != null) {
			Expression expression;
			if (value instanceof Expression) {
				expression = (Expression) value;
			}
			else if (value instanceof String) {
				expression = new LiteralExpression((String) value);
			}
			else {
				throw new IllegalArgumentException("Values must be "
						+ "'java.lang.String' or 'org.springframework.expression.Expression'; the value type for key "
						+ key + " is : " + value.getClass());
			}
			return this.evaluationCallback.evaluate(expression);
		}
		return null;
	}

	@Override
	public Collection<Object> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(Object key) {
		return original.containsKey(key);
	}

	@Override
	public Set<String> keySet() {
		return original.keySet();
	}

	@Override
	public boolean isEmpty() {
		return original.isEmpty();
	}

	@Override
	public int size() {
		return original.size();
	}

	@Override
	public boolean equals(Object o) {
		return original.equals(o);
	}

	@Override
	public int hashCode() {
		return original.hashCode();
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ?> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return this.original.toString();
	}

	public static ExpressionEvalMapBuilder from(Map<String, ?> expressions) {
		Assert.notNull(expressions, "'expressions' must not be null.");
		return new ExpressionEvalMapBuilder(expressions);
	}


	/**
	 * Implementations of this interface can be provided to build 'on demand {@link #get(Object)} logic'
	 * for {@link ExpressionEvalMap}.
	 */
	public interface EvaluationCallback {

		Object evaluate(Expression expression);

	}


	/**
	 * The {@link EvaluationCallback} implementation which evaluates an expression using
	 * the provided {@code context}, {@code root} and {@code returnType} variables.
	 */
	public static class ComponentsEvaluationCallback implements EvaluationCallback {

		private final EvaluationContext context;

		private final Object root;

		private final Class<?> returnType;

		public ComponentsEvaluationCallback(EvaluationContext context, Object root, Class<?> returnType) {
			this.context = context;
			this.root = root;
			this.returnType = returnType;
		}

		@Override
		public Object evaluate(Expression expression) {
			if (this.context != null) {
				return expression.getValue(this.context, this.root, this.returnType);
			}
			return expression.getValue(this.root, this.returnType);
		}

	}


	/**
	 * The builder class to instantiate {@link ExpressionEvalMap}.
	 */
	public static final class ExpressionEvalMapBuilder {

		private final Map<String, ?> expressions;

		private EvaluationCallback evaluationCallback;

		private EvaluationContext context;

		private Object root;

		private Class<?> returnType;

		private final ExpressionEvalMapComponentsBuilder evalMapComponentsBuilder = new ExpressionEvalMapComponentsBuilderImpl();

		private final ExpressionEvalMapFinalBuilder finalBuilder = new ExpressionEvalMapFinalBuilderImpl();

		private ExpressionEvalMapBuilder(Map<String, ?> expressions) {
			this.expressions = expressions;
		}

		public ExpressionEvalMapFinalBuilder usingCallback(EvaluationCallback callback) {
			this.evaluationCallback = callback;
			return finalBuilder;
		}

		public ExpressionEvalMapFinalBuilder usingSimpleCallback() {
			return this.usingCallback(SIMPLE_CALLBACK);
		}

		public ExpressionEvalMapComponentsBuilder usingEvaluationContext(EvaluationContext context) {
			this.context = context;
			return this.evalMapComponentsBuilder;
		}

		public ExpressionEvalMapComponentsBuilder withRoot(Object root) {
			this.root = root;
			return this.evalMapComponentsBuilder;

		}

		public ExpressionEvalMapComponentsBuilder withReturnType(Class<?> returnType) {
			this.returnType = returnType;
			return this.evalMapComponentsBuilder;

		}


		private class ExpressionEvalMapFinalBuilderImpl implements ExpressionEvalMapFinalBuilder {

			@Override
			public ExpressionEvalMap build() {
				if (evaluationCallback != null) {
					return new ExpressionEvalMap(expressions, evaluationCallback);
				}
				return new ExpressionEvalMap(expressions, new ComponentsEvaluationCallback(context, root, returnType));
			}

		}


		private class ExpressionEvalMapComponentsBuilderImpl extends ExpressionEvalMapFinalBuilderImpl
				implements ExpressionEvalMapComponentsBuilder {

			@Override
			public ExpressionEvalMapComponentsBuilder usingEvaluationContext(EvaluationContext context) {
				return ExpressionEvalMapBuilder.this.usingEvaluationContext(context);
			}

			@Override
			public ExpressionEvalMapComponentsBuilder withRoot(Object root) {
				return ExpressionEvalMapBuilder.this.withRoot(root);
			}

			@Override
			public ExpressionEvalMapComponentsBuilder withReturnType(Class<?> returnType) {
				return ExpressionEvalMapBuilder.this.withReturnType(returnType);
			}

		}

	}


	public interface ExpressionEvalMapFinalBuilder {

		ExpressionEvalMap build();

	}


	public interface ExpressionEvalMapComponentsBuilder extends ExpressionEvalMapFinalBuilder {

		ExpressionEvalMapComponentsBuilder usingEvaluationContext(EvaluationContext context);

		ExpressionEvalMapComponentsBuilder withRoot(Object root);

		ExpressionEvalMapComponentsBuilder withReturnType(Class<?> returnType);

	}

}
