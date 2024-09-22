/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.expression.Expression;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.handler.BeanNameMessageProcessor;
import org.springframework.integration.handler.LambdaMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.transformer.ExpressionEvaluatingTransformer;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MethodInvokingTransformer;
import org.springframework.integration.transformer.Transformer;
import org.springframework.integration.util.ClassUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link ConsumerEndpointSpec} for a {@link MessageTransformingHandler} options.
 * One of the {@link #expression(String)}, {@link #ref(Object)}, {@link #refName(String)},
 * {@link #processor(MessageProcessorSpec)} or {@link #transformer(GenericTransformer)} must be provided.
 *
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 6.2
 */
public class TransformerEndpointSpec extends ConsumerEndpointSpec<TransformerEndpointSpec, MessageTransformingHandler> {

	private final AtomicBoolean transformerSet = new AtomicBoolean();

	private Expression expression;

	private Object ref;

	private String refName;

	@Nullable
	private String method;

	private GenericTransformer<?, ?> transformer;

	@Nullable
	private Class<?> expectedType;

	private MessageProcessorSpec<?> processor;

	protected TransformerEndpointSpec() {
		super(new MessageTransformingHandler());
	}

	/**
	 * Provide an expression to use an {@link ExpressionEvaluatingTransformer} for the target handler.
	 * @param expression the SpEL expression to use.
	 * @return the TransformerSpec
	 */
	public TransformerEndpointSpec expression(String expression) {
		return expression(PARSER.parseExpression(expression));
	}

	/**
	 * Provide an expression to use an {@link ExpressionEvaluatingTransformer} for the target handler.
	 * @param expression the SpEL expression to use.
	 * @return the TransformerSpec
	 */
	public TransformerEndpointSpec expression(Expression expression) {
		assertTransformerSet();
		this.expression = expression;
		return this;
	}

	/**
	 * Provide a service to use a {@link MethodInvokingTransformer} for the target handler.
	 * @param ref the service to call as a transformer POJO.
	 * @return the TransformerSpec
	 */
	public TransformerEndpointSpec ref(Object ref) {
		assertTransformerSet();
		this.ref = ref;
		return this;
	}

	/**
	 * Provide a bean name to use a {@link MethodInvokingTransformer}
	 * (based on {@link BeanNameMessageProcessor}) for the target handler.
	 * @param refName the bean name for service to call as a transformer POJO.
	 * @return the TransformerSpec
	 */
	public TransformerEndpointSpec refName(String refName) {
		assertTransformerSet();
		this.refName = refName;
		return this;
	}

	/**
	 * Provide a service method name to call. Optional.
	 * Use only together with {@link #ref(Object)} or {@link #refName(String)}.
	 * @param method the service method name to call.
	 * @return the TransformerSpec
	 */
	public TransformerEndpointSpec method(@Nullable String method) {
		this.method = method;
		return this;
	}

	/**
	 * Provide a {@link GenericTransformer} as a direct delegate for {@link MessageTransformingHandler}.
	 * @param transformer the {@link GenericTransformer} instance to use.
	 * @param <P> the input type.
	 * @param <T> the output type.
	 * @return the TransformerSpec
	 */
	public <P, T> TransformerEndpointSpec transformer(GenericTransformer<P, T> transformer) {
		assertTransformerSet();
		this.transformer = transformer;
		return this;
	}

	/**
	 * Set a {@link GenericTransformer} input argument type.
	 * Can be a {@link org.springframework.messaging.Message}.
	 * Ignored for all other transformers, but {@link #transformer(GenericTransformer)}.
	 * @param expectedType the {@link GenericTransformer} input argument type.
	 * @param <P> the type ot expect.
	 * @return the spec.
	 */
	public <P> TransformerEndpointSpec expectedType(@Nullable Class<P> expectedType) {
		this.expectedType = expectedType;
		return this;
	}

	/**
	 * Provide a {@link MessageProcessorSpec} as a factory for {@link MethodInvokingTransformer} delegate.
	 * @param processor the {@link MessageProcessorSpec} to use.
	 * @return the TransformerSpec
	 */
	public TransformerEndpointSpec processor(MessageProcessorSpec<?> processor) {
		assertTransformerSet();
		this.processor = processor;
		return this;
	}

	private void assertTransformerSet() {
		Assert.isTrue(this.transformerSet.compareAndSet(false, true), this::assertMessage);
	}

	private String assertMessage() {
		String currentTransformerValue = null;
		if (this.expression != null) {
			currentTransformerValue = "'expression'=" + this.expression;
		}
		else if (this.ref != null) {
			currentTransformerValue = "'ref'=" + this.ref;
		}
		else if (this.refName != null) {
			currentTransformerValue = "'refName'=" + this.refName;
		}
		else if (this.transformer != null) {
			currentTransformerValue = "'transformer'=" + this.transformer;
		}
		else if (this.processor != null) {
			currentTransformerValue = "'processor'=" + this.processor;
		}
		return "Only one of the 'expression', 'ref', 'refName', 'processor' or 'transformer' can be set. " +
				"Current one is " + currentTransformerValue;
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		Transformer transformer;
		if (this.expression != null) {
			transformer = new ExpressionEvaluatingTransformer(this.expression);
		}
		else if (this.ref != null) {
			if (this.method != null) {
				transformer = new MethodInvokingTransformer(this.ref, this.method);
			}
			else {
				transformer = new MethodInvokingTransformer(this.ref);
			}
		}
		else if (this.refName != null) {
			transformer = new MethodInvokingTransformer(new BeanNameMessageProcessor<>(this.refName, this.method));
		}
		else if (this.processor != null) {
			MessageProcessor<?> targetProcessor = this.processor.getObject();
			this.componentsToRegister.put(targetProcessor, null);
			transformer = new MethodInvokingTransformer(targetProcessor);
		}
		else if (this.transformer != null) {
			transformer = wrapToTransformerIfAny();
		}
		else {
			throw new IllegalStateException(
					"One of the 'expression', 'ref', 'refName', 'processor' or 'transformer' must be provided.");
		}

		this.handler.setTransformer(transformer);

		this.componentsToRegister.put(transformer, null);
		return super.getComponentsToRegister();
	}

	private Transformer wrapToTransformerIfAny() {
		return this.transformer instanceof Transformer castTransformer ? castTransformer :
				(ClassUtils.isLambda(this.transformer)
						? new MethodInvokingTransformer(new LambdaMessageProcessor(this.transformer, this.expectedType))
						: new MethodInvokingTransformer(this.transformer, ClassUtils.TRANSFORMER_TRANSFORM_METHOD));
	}

}
