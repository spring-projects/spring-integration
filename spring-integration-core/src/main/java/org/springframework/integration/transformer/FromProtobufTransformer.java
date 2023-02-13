/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.integration.transformer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.Map;

import com.google.protobuf.Message.Builder;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.transformer.support.ProtoHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * A Protocol Buffer transformer to instantiate {@link com.google.protobuf.Message} objects from {@code byte[]}.
 *
 * @author Christian Tzolov
 * @since 6.1
 */
public class FromProtobufTransformer extends AbstractTransformer implements BeanClassLoaderAware {

	private static final Map<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

	private final Class<? extends com.google.protobuf.Message> defaultType;

	private ClassLoader beanClassLoader;

	private Expression typeIdExpression = new FunctionExpression<Message<?>>(
			(message) -> message.getHeaders().get(ProtoHeaders.TYPE));

	private EvaluationContext evaluationContext;

	/**
	 * Construct an instance with the supplied default type to create.
	 * @param defaultType the type.
	 */
	public FromProtobufTransformer(Class<? extends com.google.protobuf.Message> defaultType) {
		Assert.notNull(defaultType, "'defaultType' must not be null");
		this.defaultType = defaultType;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type. Default {@code headers['proto_type']}.
	 * @param expression the expression.
	 */
	public void setTypeExpression(Expression expression) {
		assertExpressionNotNull(expression);
		this.typeIdExpression = expression;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type id. Default
	 * {@code headers['proto_type']}.
	 * @param expression the expression.
	 */
	public void setTypeExpressionString(String expression) {
		assertExpressionNotNull(expression);
		this.typeIdExpression = EXPRESSION_PARSER.parseExpression(expression);
	}

	private void assertExpressionNotNull(Object expression) {
		Assert.notNull(expression, "'expression' must not be null");
	}

	@Override
	protected void onInit() {
		this.evaluationContext = IntegrationContextUtils.getEvaluationContext(getBeanFactory());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Object doTransform(Message<?> message) {
		Assert.state(message.getPayload() instanceof byte[], "Payload must be a byte[]");
		Class<? extends com.google.protobuf.Message> type = null;
		Object value = this.typeIdExpression.getValue(this.evaluationContext, message);
		if (value instanceof Class) {
			type = (Class<? extends com.google.protobuf.Message>) value;
		}
		else if (value instanceof String) {
			try {
				type = (Class<? extends com.google.protobuf.Message>) ClassUtils.forName((String) value,
						this.beanClassLoader);
			}
			catch (ClassNotFoundException | LinkageError e) {
				throw new IllegalStateException(e);
			}
		}
		if (type == null) {
			type = this.defaultType;
		}

		try {
			Builder messageBuilder = getMessageBuilder(type);
			byte[] payload = (byte[]) message.getPayload();
			return messageBuilder.mergeFrom(payload).build();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Create a new {@code Message.Builder} instance for the given class.
	 * <p>
	 * This method uses a ConcurrentReferenceHashMap for caching method lookups.
	 */
	private com.google.protobuf.Message.Builder getMessageBuilder(Class<? extends com.google.protobuf.Message> clazz) {

		Method method = methodCache.computeIfAbsent(clazz, clz -> {
			try {
				// The com.google.protobuf.Message interface doesn't define an explicit newBuilder method.
				// But all generated message classes that implement the Message interface implement the
				// newBuilder method as well.
				return clz.getMethod("newBuilder");
			}
			catch (Exception ex) {
				throw new RuntimeException("No newBuilder() method fond for Class: " + clazz, ex);
			}
		});

		try {
			return (com.google.protobuf.Message.Builder) method.invoke(clazz);
		}
		catch (Exception ex) {
			throw new RuntimeException(
					"Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, ex);
		}
	}
}
