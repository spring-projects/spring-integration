/*
 * Copyright 2023 the original author or authors.
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import reactor.util.function.Tuple2;

import org.springframework.expression.Expression;
import org.springframework.integration.JavaUtils;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.handler.BeanNameMessageProcessor;
import org.springframework.integration.handler.LambdaMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.splitter.ExpressionEvaluatingSplitter;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.integration.util.ClassUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * A {@link ConsumerEndpointSpec} for an {@link AbstractMessageSplitter}.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class SplitterSpec extends ConsumerEndpointSpec<SplitterSpec, AbstractMessageSplitter> {

	private final AtomicBoolean splitterSet = new AtomicBoolean();

	private Expression expression;

	private Object ref;

	private String refName;

	@Nullable
	private String method;

	private Function<?, ?> function;

	@Nullable
	private Class<?> expectedType;

	@Nullable
	private String delimiters;

	@Nullable
	private String discardChannelName;

	@Nullable
	private MessageChannel discardChannel;

	@Nullable
	private Boolean applySequence;

	protected SplitterSpec() {
		super(null);
	}

	/**
	 * Set delimiters to tokenize String values. The default is
	 * <code>null</code> indicating that no tokenizing should occur.
	 * If delimiters are provided, they will be applied to any String payload.
	 * Only applied if provided {@code splitter} is instance of {@link DefaultMessageSplitter}.
	 * @param delimiters The delimiters.
	 * @return the endpoint spec.
	 * @see DefaultMessageSplitter#setDelimiters(String)
	 */
	public SplitterSpec delimiters(String delimiters) {
		this.delimiters = delimiters;
		return this;
	}

	/**
	 * Provide an expression to use an {@link ExpressionEvaluatingSplitter} for the target handler.
	 * @param expression the SpEL expression to use.
	 * @return the spec
	 */
	public SplitterSpec expression(String expression) {
		return expression(PARSER.parseExpression(expression));
	}

	/**
	 * Provide an expression to use an {@link ExpressionEvaluatingSplitter} for the target handler.
	 * @param expression the SpEL expression to use.
	 * @return the spec
	 */
	public SplitterSpec expression(Expression expression) {
		assertSplitterSet();
		this.expression = expression;
		return this;
	}

	/**
	 * Provide a service to use a {@link MethodInvokingSplitter} for the target handler.
	 * This option can be set to an {@link AbstractMessageSplitter} implementation,
	 * a {@link MessageHandlerSpec} providing an {@link AbstractMessageSplitter},
	 * or {@link MessageProcessorSpec}.
	 * @param ref the service to call as a splitter POJO.
	 * @return the spec
	 */
	public SplitterSpec ref(Object ref) {
		assertSplitterSet();
		this.ref = ref;
		return this;
	}

	/**
	 * Provide a bean name to use a {@link MethodInvokingSplitter}
	 * (based on {@link BeanNameMessageProcessor}) for the target handler.
	 * @param refName the bean name for service to call as a splitter POJO.
	 * @return the spec
	 */
	public SplitterSpec refName(String refName) {
		assertSplitterSet();
		this.refName = refName;
		return this;
	}

	/**
	 * Provide a service method name to call. Optional.
	 * Use only together with {@link #ref(Object)} or {@link #refName(String)}.
	 * @param method the service method name to call.
	 * @return the spec
	 */
	public SplitterSpec method(@Nullable String method) {
		this.method = method;
		return this;
	}

	/**
	 * Provide a {@link Function} as a direct delegate for {@link MethodInvokingSplitter}.
	 * @param function the {@link Function} instance to use.
	 * @param <P> the input type.
	 * @return the spec
	 */
	public <P> SplitterSpec function(Function<P, ?> function) {
		assertSplitterSet();
		this.function = function;
		return this;
	}

	/**
	 * Set a {@link Function} input argument type.
	 * Can be a {@link org.springframework.messaging.Message}.
	 * Ignored for all other options, but {@link #function(Function)}.
	 * @param expectedType the {@link Function} input argument type.
	 * @return the spec.
	 */
	public SplitterSpec expectedType(@Nullable Class<?> expectedType) {
		this.expectedType = expectedType;
		return this;
	}

	/**
	 * Set the applySequence flag to the specified value. Defaults to {@code true}.
	 * @param applySequence the applySequence.
	 * @return the endpoint spec.
	 * @see AbstractMessageSplitter#setApplySequence(boolean)
	 */
	public SplitterSpec applySequence(boolean applySequence) {
		this.applySequence = applySequence;
		return this;
	}

	/**
	 * Specify a channel where rejected Messages should be sent. If the discard
	 * channel is null (the default), rejected Messages will be dropped.
	 * A "Rejected Message" means that split function has returned an empty result (but not null):
	 * no items to iterate for sending.
	 * @param discardChannel The discard channel.
	 * @return the endpoint spec.
	 * @see DefaultMessageSplitter#setDelimiters(String)
	 */
	public SplitterSpec discardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
		return this;
	}

	/**
	 * Configure a subflow to run for discarded messages instead of a
	 * {@link #discardChannel(MessageChannel)}.
	 * @param discardFlow the discard flow.
	 * @return the endpoint spec.
	 */
	public SplitterSpec discardFlow(IntegrationFlow discardFlow) {
		return discardChannel(obtainInputChannelFromFlow(discardFlow));
	}

	/**
	 * Specify a channel bean name where rejected Messages should be sent. If the discard
	 * channel is null (the default), rejected Messages will be dropped.
	 * A "Rejected Message" means that split function has returned an empty result (but not null):
	 * no items to iterate for sending.
	 * @param discardChannelName The discard channel bean name.
	 * @return the endpoint spec.
	 * @see DefaultMessageSplitter#setDelimiters(String)
	 */
	public SplitterSpec discardChannel(String discardChannelName) {
		this.discardChannelName = discardChannelName;
		return this;
	}

	private void assertSplitterSet() {
		Assert.isTrue(this.splitterSet.compareAndSet(false, true), this::assertMessage);
	}

	private String assertMessage() {
		String currentSplitterValue = null;
		if (this.expression != null) {
			currentSplitterValue = "'expression'=" + this.expression;
		}
		else if (this.ref != null) {
			currentSplitterValue = "'ref'=" + this.ref;
		}
		else if (this.refName != null) {
			currentSplitterValue = "'refName'=" + this.refName;
		}
		else if (this.function != null) {
			currentSplitterValue = "'function'=" + this.function;
		}
		return "Only one of the 'expression', 'ref', 'refName', 'function' can be set. " +
				"Current one is " + currentSplitterValue;
	}

	@Override
	public Tuple2<ConsumerEndpointFactoryBean, AbstractMessageSplitter> doGet() {
		AbstractMessageSplitter splitter = new DefaultMessageSplitter();
		if (this.expression != null) {
			splitter = new ExpressionEvaluatingSplitter(this.expression);
		}
		else if (this.ref != null) {
			if (this.method != null) {
				splitter = new MethodInvokingSplitter(this.ref, this.method);
			}
			else if (this.ref instanceof MessageProcessorSpec<?> messageProcessorSpec) {
				MessageProcessor<?> targetProcessor = messageProcessorSpec.getObject();
				this.componentsToRegister.put(targetProcessor, null);
				splitter = new MethodInvokingSplitter(targetProcessor);
			}
			else if (this.ref instanceof MessageHandlerSpec<?, ?> messageHandlerSpec) {
				MessageHandler messageHandler = messageHandlerSpec.getObject();
				Assert.isInstanceOf(AbstractMessageSplitter.class, messageHandler,
						"Only the 'MessageHandlerSpec' producing an `AbstractMessageSplitter` can be used as a `ref`. " +
								"All others should be used in a `.handle()`.");
				splitter = (AbstractMessageSplitter) messageHandler;
			}
			else if (this.ref instanceof AbstractMessageSplitter messageSplitter) {
				splitter = messageSplitter;
			}
			else {
				splitter = new MethodInvokingSplitter(this.ref);
			}
		}
		else if (this.refName != null) {
			splitter = new MethodInvokingSplitter(new BeanNameMessageProcessor<>(this.refName, this.method));
		}
		else if (this.function != null) {
			splitter = wrapFunctionToSplitter();
		}

		if (this.delimiters != null) {
			if (splitter instanceof DefaultMessageSplitter defaultMessageSplitter) {
				defaultMessageSplitter.setDelimiters(this.delimiters);
			}
			else {
				logger.warn("'delimiters' can be applied only for the DefaultMessageSplitter");
			}
		}

		JavaUtils.INSTANCE
				.acceptIfNotNull(this.discardChannel, splitter::setDiscardChannel)
				.acceptIfHasText(this.discardChannelName, splitter::setDiscardChannelName)
				.acceptIfNotNull(this.applySequence, splitter::setApplySequence);

		this.handler = splitter;

		return super.doGet();
	}

	private MethodInvokingSplitter wrapFunctionToSplitter() {
		return ClassUtils.isLambda(this.function)
				? new MethodInvokingSplitter(new LambdaMessageProcessor(this.function, this.expectedType))
				: new MethodInvokingSplitter(this.function, ClassUtils.FUNCTION_APPLY_METHOD);
	}

}
