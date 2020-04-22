/*
 * Copyright 2019-2020 the original author or authors.
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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.channel.BroadcastCapableChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.dsl.support.FixedSubscriberChannelPrototype;
import org.springframework.integration.dsl.support.MessageChannelReference;
import org.springframework.integration.expression.ControlBusMethodFilter;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.BeanNameMessageProcessor;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.handler.ExpressionCommandMessageProcessor;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LambdaMessageProcessor;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MessageTriggerAction;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.ErrorMessageExceptionTypeRouter;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.scattergather.ScatterGatherHandler;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.splitter.ExpressionEvaluatingSplitter;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MapBuilder;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.integration.transformer.ClaimCheckOutTransformer;
import org.springframework.integration.transformer.ExpressionEvaluatingTransformer;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.integration.transformer.HeaderFilter;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MethodInvokingTransformer;
import org.springframework.integration.transformer.Transformer;
import org.springframework.integration.util.ClassUtils;
import org.springframework.integration.util.IntegrationReactiveUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

/**
 * The {@code Builder} pattern implementation for the EIP-method chain.
 * Provides a variety of methods to populate Spring Integration components
 * to an {@link IntegrationFlow} for the future registration in the
 * application context.
 *
 * @param <B> the {@link BaseIntegrationFlowDefinition} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Gabriele Del Prete
 * @author Tim Feuerbach
 *
 * @since 5.2.1
 *
 * @see org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor
 */
public abstract class BaseIntegrationFlowDefinition<B extends BaseIntegrationFlowDefinition<B>> {

	private static final String UNCHECKED = "unchecked";

	private static final String FUNCTION_MUST_NOT_BE_NULL = "'function' must not be null";

	private static final String MESSAGE_PROCESSOR_SPEC_MUST_NOT_BE_NULL = "'messageProcessorSpec' must not be null";

	private static final Set<MessageProducer> REFERENCED_REPLY_PRODUCERS = new HashSet<>();

	protected static final SpelExpressionParser PARSER = new SpelExpressionParser(); //NOSONAR - final

	protected final Map<Object, String> integrationComponents = new LinkedHashMap<>(); //NOSONAR - final

	private MessageChannel currentMessageChannel;

	private Object currentComponent;

	private boolean implicitChannel;

	private StandardIntegrationFlow integrationFlow;

	protected BaseIntegrationFlowDefinition() {
	}

	protected B addComponent(Object component) {
		return addComponent(component, null);
	}

	protected B addComponent(Object component, @Nullable String beanName) {
		this.integrationComponents.put(component, beanName);
		return _this();
	}

	protected B addComponents(Map<Object, String> components) {
		if (components != null) {
			this.integrationComponents.putAll(components);
		}
		return _this();
	}

	protected Map<Object, String> getIntegrationComponents() {
		return this.integrationComponents;
	}

	protected B currentComponent(@Nullable Object component) {
		this.currentComponent = component;
		return _this();
	}

	@Nullable
	protected Object getCurrentComponent() {
		return this.currentComponent;
	}

	protected B currentMessageChannel(@Nullable MessageChannel currentMessageChannel) {
		this.currentMessageChannel = currentMessageChannel;
		return _this();
	}

	@Nullable
	protected MessageChannel getCurrentMessageChannel() {
		return this.currentMessageChannel;
	}

	/**
	 * Return the current channel if it is an {@link InterceptableChannel}, otherwise register a new implicit
	 * {@link DirectChannel} in the flow and return that one.
	 * @return the current channel after the operation
	 */
	protected InterceptableChannel currentInterceptableChannel() {
		MessageChannel currentChannel = getCurrentMessageChannel();
		if (currentChannel instanceof InterceptableChannel) {
			return (InterceptableChannel) currentChannel;
		}
		else {
			DirectChannel newCurrentChannel = new DirectChannel();
			channel(newCurrentChannel);
			setImplicitChannel(true);
			return newCurrentChannel;
		}
	}

	protected void setImplicitChannel(boolean implicitChannel) {
		this.implicitChannel = implicitChannel;
	}

	protected boolean isImplicitChannel() {
		return this.implicitChannel;
	}

	/**
	 * Populate an {@link org.springframework.integration.channel.FixedSubscriberChannel} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The 'bean name' will be generated during the bean registration phase.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B fixedSubscriberChannel() {
		return fixedSubscriberChannel(null);
	}

	/**
	 * Populate an {@link org.springframework.integration.channel.FixedSubscriberChannel} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The provided {@code messageChannelName} is used for the bean registration.
	 * @param messageChannelName the bean name to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B fixedSubscriberChannel(String messageChannelName) {
		return channel(new FixedSubscriberChannelPrototype(messageChannelName));
	}

	/**
	 * Populate a {@link MessageChannelReference} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The provided {@code messageChannelName} is used for the bean registration
	 * ({@link org.springframework.integration.channel.DirectChannel}), if there is no such a bean
	 * in the application context. Otherwise the existing {@link MessageChannel} bean is used
	 * to wire integration endpoints.
	 * @param messageChannelName the bean name to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B channel(String messageChannelName) {
		return channel(new MessageChannelReference(messageChannelName));
	}

	/**
	 * Populate a {@link MessageChannel} instance
	 * at the current {@link IntegrationFlow} chain position using the {@link MessageChannelSpec}
	 * fluent API.
	 * @param messageChannelSpec the {@link MessageChannelSpec} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MessageChannels
	 */
	public B channel(MessageChannelSpec<?, ?> messageChannelSpec) {
		Assert.notNull(messageChannelSpec, "'messageChannelSpec' must not be null");
		return channel(messageChannelSpec.get());
	}

	/**
	 * Populate the provided {@link MessageChannel} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The {@code messageChannel} can be an existing bean, or fresh instance, in which case
	 * the {@link org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor}
	 * will populate it as a bean with a generated name.
	 * @param messageChannel the {@link MessageChannel} to populate.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B channel(MessageChannel messageChannel) {
		Assert.notNull(messageChannel, "'messageChannel' must not be null");
		setImplicitChannel(false);
		if (getCurrentMessageChannel() != null) {
			bridge();
		}
		currentMessageChannel(messageChannel);
		return registerOutputChannelIfCan(messageChannel);
	}

	/**
	 * Populate a {@link MessageChannel} instance
	 * at the current {@link IntegrationFlow} chain position using the {@link Channels}
	 * factory fluent API.
	 * @param channels the {@link Function} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B channel(Function<Channels, MessageChannelSpec<?, ?>> channels) {
		Assert.notNull(channels, "'channels' must not be null");
		return channel(channels.apply(Channels.INSTANCE));
	}

	/**
	 * The {@link org.springframework.integration.channel.PublishSubscribeChannel} {@link #channel}
	 * method specific implementation to allow the use of the 'subflow' subscriber capability.
	 * @param publishSubscribeChannelConfigurer the {@link Consumer} to specify
	 * {@link PublishSubscribeSpec} options including 'subflow' definition.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B publishSubscribeChannel(Consumer<PublishSubscribeSpec> publishSubscribeChannelConfigurer) {
		return publishSubscribeChannel(null, publishSubscribeChannelConfigurer);
	}

	/**
	 * The {@link org.springframework.integration.channel.PublishSubscribeChannel} {@link #channel}
	 * method specific implementation to allow the use of the 'subflow' subscriber capability.
	 * Use the provided {@link Executor} for the target subscribers.
	 * @param executor the {@link Executor} to use.
	 * @param publishSubscribeChannelConfigurer the {@link Consumer} to specify
	 * {@link PublishSubscribeSpec} options including 'subflow' definition.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B publishSubscribeChannel(Executor executor,
			Consumer<PublishSubscribeSpec> publishSubscribeChannelConfigurer) {

		Assert.notNull(publishSubscribeChannelConfigurer, "'publishSubscribeChannelConfigurer' must not be null");
		PublishSubscribeSpec spec = new PublishSubscribeSpec(executor);
		publishSubscribeChannelConfigurer.accept(spec);
		return addComponents(spec.getComponentsToRegister()).channel(spec);
	}

	/**
	 * The {@link BroadcastCapableChannel} {@link #channel}
	 * method specific implementation to allow the use of the 'subflow' subscriber capability.
	 * @param broadcastCapableChannel the {@link BroadcastCapableChannel} to subscriber sub-flows to.
	 * @param publishSubscribeChannelConfigurer the {@link Consumer} to specify
	 * {@link BroadcastPublishSubscribeSpec} 'subflow' definitions.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @since 5.3
	 */
	public B publishSubscribeChannel(BroadcastCapableChannel broadcastCapableChannel,
			Consumer<BroadcastPublishSubscribeSpec> publishSubscribeChannelConfigurer) {

		Assert.notNull(publishSubscribeChannelConfigurer, "'publishSubscribeChannelConfigurer' must not be null");
		BroadcastPublishSubscribeSpec spec = new BroadcastPublishSubscribeSpec(broadcastCapableChannel);
		publishSubscribeChannelConfigurer.accept(spec);
		return addComponents(spec.getComponentsToRegister())
				.channel(broadcastCapableChannel);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .filter("World"::equals)
	 *  .wireTap(sf -> sf.<String, String>transform(String::toUpperCase))
	 *  .handle(p -> process(p))
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param flow the {@link IntegrationFlow} for wire-tap subflow as an alternative to the {@code wireTapChannel}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B wireTap(IntegrationFlow flow) {
		return wireTap(flow, null);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  f -> f.wireTap("tapChannel")
	 *    .handle(p -> process(p))
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param wireTapChannel the {@link MessageChannel} bean name to wire-tap.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B wireTap(String wireTapChannel) {
		return wireTap(wireTapChannel, null);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .transform("payload")
	 *  .wireTap(tapChannel())
	 *  .channel("foo")
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param wireTapChannel the {@link MessageChannel} to wire-tap.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B wireTap(MessageChannel wireTapChannel) {
		return wireTap(wireTapChannel, null);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .transform("payload")
	 *  .wireTap(sf -> sf.<String, String>transform(String::toUpperCase), wt -> wt.selector("payload == 'foo'"))
	 *  .channel("foo")
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param flow the {@link IntegrationFlow} for wire-tap subflow as an alternative to the {@code wireTapChannel}.
	 * @param wireTapConfigurer the {@link Consumer} to accept options for the {@link WireTap}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B wireTap(IntegrationFlow flow, Consumer<WireTapSpec> wireTapConfigurer) {
		MessageChannel wireTapChannel = obtainInputChannelFromFlow(flow);

		return wireTap(wireTapChannel, wireTapConfigurer);
	}

	protected MessageChannel obtainInputChannelFromFlow(IntegrationFlow flow) {
		Assert.notNull(flow, "'flow' must not be null");
		MessageChannel messageChannel = flow.getInputChannel();
		if (messageChannel == null) {
			messageChannel = new DirectChannel();
			IntegrationFlowDefinition<?> flowBuilder = IntegrationFlows.from(messageChannel);
			flow.configure(flowBuilder);
			addComponent(flowBuilder.get());
		}
		else {
			addComponent(flow);
		}

		return messageChannel;
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .transform("payload")
	 *  .wireTap("tapChannel", wt -> wt.selector(m -> m.getPayload().equals("foo")))
	 *  .channel("foo")
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param wireTapChannel the {@link MessageChannel} bean name to wire-tap.
	 * @param wireTapConfigurer the {@link Consumer} to accept options for the {@link WireTap}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B wireTap(String wireTapChannel, Consumer<WireTapSpec> wireTapConfigurer) {
		DirectChannel internalWireTapChannel = new DirectChannel();
		addComponent(IntegrationFlows.from(internalWireTapChannel).channel(wireTapChannel).get());
		return wireTap(internalWireTapChannel, wireTapConfigurer);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .transform("payload")
	 *  .wireTap(tapChannel(), wt -> wt.selector(m -> m.getPayload().equals("foo")))
	 *  .channel("foo")
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param wireTapChannel the {@link MessageChannel} to wire-tap.
	 * @param wireTapConfigurer the {@link Consumer} to accept options for the {@link WireTap}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B wireTap(MessageChannel wireTapChannel, Consumer<WireTapSpec> wireTapConfigurer) {
		WireTapSpec wireTapSpec = new WireTapSpec(wireTapChannel);
		if (wireTapConfigurer != null) {
			wireTapConfigurer.accept(wireTapSpec);
		}
		addComponent(wireTapChannel);
		return wireTap(wireTapSpec);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * <p> It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .transform("payload")
	 *  .wireTap(new WireTap(tapChannel().selector(m -> m.getPayload().equals("foo")))
	 *  .channel("foo")
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param wireTapSpec the {@link WireTapSpec} to use.
	 * <p> When this EIP-method is used in the end of flow, it appends {@code nullChannel} to terminate flow properly,
	 * Otherwise {@code Dispatcher has no subscribers} exception is thrown for implicit {@link DirectChannel}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B wireTap(WireTapSpec wireTapSpec) {
		WireTap interceptor = wireTapSpec.get();
		InterceptableChannel currentChannel = currentInterceptableChannel();
		addComponent(wireTapSpec);
		currentChannel.addInterceptor(interceptor);
		return _this();
	}

	/**
	 * Populate the {@code Control Bus} EI Pattern specific {@link MessageHandler} implementation
	 * at the current {@link IntegrationFlow} chain position.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see ExpressionCommandMessageProcessor
	 */
	public B controlBus() {
		return controlBus(null);
	}

	/**
	 * Populate the {@code Control Bus} EI Pattern specific {@link MessageHandler} implementation
	 * at the current {@link IntegrationFlow} chain position.
	 * @param endpointConfigurer the {@link Consumer} to accept integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see ExpressionCommandMessageProcessor
	 * @see GenericEndpointSpec
	 */
	public B controlBus(Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {
		return handle(new ServiceActivatingHandler(new ExpressionCommandMessageProcessor(
				new ControlBusMethodFilter())), endpointConfigurer);
	}

	/**
	 * Populate the {@code Transformer} EI Pattern specific {@link MessageHandler} implementation
	 * for the SpEL {@link Expression}.
	 * @param expression the {@code Transformer} {@link Expression}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see ExpressionEvaluatingTransformer
	 */
	public B transform(String expression) {
		return transform(expression, (Consumer<GenericEndpointSpec<MessageTransformingHandler>>) null);
	}

	/**
	 * Populate the {@code Transformer} EI Pattern specific {@link MessageHandler} implementation
	 * for the SpEL {@link Expression}.
	 * @param expression the {@code Transformer} {@link Expression}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see ExpressionEvaluatingTransformer
	 */
	public B transform(String expression,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		Assert.hasText(expression, "'expression' must not be empty");
		return transform(null,
				new ExpressionEvaluatingTransformer(PARSER.parseExpression(expression)),
				endpointConfigurer);
	}

	/**
	 * Populate the {@code MessageTransformingHandler} for the {@link MethodInvokingTransformer}
	 * to invoke the discovered service method at runtime.
	 * @param service the service to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see ExpressionEvaluatingTransformer
	 */
	public B transform(Object service) {
		return transform(service, null);
	}

	/**
	 * Populate the {@code MessageTransformingHandler} for the {@link MethodInvokingTransformer}
	 * to invoke the service method at runtime.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingTransformer
	 */
	public B transform(Object service, String methodName) {
		return transform(service, methodName, null);
	}

	/**
	 * Populate the {@code MessageTransformingHandler} for the {@link MethodInvokingTransformer}
	 * to invoke the service method at runtime.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see ExpressionEvaluatingTransformer
	 */
	public B transform(Object service, String methodName,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		MethodInvokingTransformer transformer;
		if (StringUtils.hasText(methodName)) {
			transformer = new MethodInvokingTransformer(service, methodName);
		}
		else {
			transformer = new MethodInvokingTransformer(service);
		}

		return transform(null, transformer, endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the
	 * {@link org.springframework.integration.handler.MessageProcessor} from provided {@link MessageProcessorSpec}.
	 * <pre class="code">
	 * {@code
	 *  .transform(Scripts.script("classpath:myScript.py").variable("foo", bar()))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingTransformer
	 */
	public B transform(MessageProcessorSpec<?> messageProcessorSpec) {
		return transform(messageProcessorSpec, (Consumer<GenericEndpointSpec<MessageTransformingHandler>>) null);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the
	 * {@link org.springframework.integration.handler.MessageProcessor} from provided {@link MessageProcessorSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * <pre class="code">
	 * {@code
	 *  .transform(Scripts.script("classpath:myScript.py").variable("foo", bar()),
	 *           e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingTransformer
	 */
	public B transform(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		Assert.notNull(messageProcessorSpec, MESSAGE_PROCESSOR_SPEC_MUST_NOT_BE_NULL);
		MessageProcessor<?> processor = messageProcessorSpec.get();
		return addComponent(processor)
				.transform(null, new MethodInvokingTransformer(processor), endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance
	 * for the provided {@code payloadType} to convert at runtime.
	 * @param payloadType the {@link Class} for expected payload type.
	 * @param <P> the payload type - 'convert to'.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @since 5.1
	 * @see MethodInvokingTransformer
	 * @see LambdaMessageProcessor
	 */
	public <P> B convert(Class<P> payloadType) {
		Assert.isTrue(!payloadType.equals(Message.class), ".convert() does not support Message as an explicit type");
		return transform(payloadType, p -> p);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the provided
	 * {@link GenericTransformer} for the specific {@code payloadType} to convert at
	 * runtime.
	 * @param payloadType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the transformer.
	 * Conversion to this type will be attempted, if necessary.
	 * @param genericTransformer the {@link GenericTransformer} to populate.
	 * @param <P> the payload type - 'transform from' or {@code Message.class}.
	 * @param <T> the target type - 'transform to'.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingTransformer
	 * @see LambdaMessageProcessor
	 */
	public <P, T> B transform(Class<P> payloadType, GenericTransformer<P, T> genericTransformer) {
		return transform(payloadType, genericTransformer, null);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance
	 * for the provided {@code payloadType} to convert at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param payloadType the {@link Class} for expected payload type.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type - 'transform to'.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @since 5.1
	 * @see MethodInvokingTransformer
	 * @see LambdaMessageProcessor
	 * @see GenericEndpointSpec
	 */
	public <P> B convert(Class<P> payloadType,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		Assert.isTrue(!payloadType.equals(Message.class), ".convert() does not support Message");
		return transform(payloadType, p -> p, endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the provided {@link GenericTransformer}
	 * for the specific {@code payloadType} to convert at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param payloadType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the transformer.
	 * Conversion to this type will be attempted, if necessary.
	 * @param genericTransformer the {@link GenericTransformer} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type - 'transform from', or {@code Message.class}.
	 * @param <T> the target type - 'transform to'.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingTransformer
	 * @see LambdaMessageProcessor
	 * @see GenericEndpointSpec
	 */
	public <P, T> B transform(Class<P> payloadType, GenericTransformer<P, T> genericTransformer,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		Assert.notNull(genericTransformer, "'genericTransformer' must not be null");
		Transformer transformer = genericTransformer instanceof Transformer ? (Transformer) genericTransformer :
				(ClassUtils.isLambda(genericTransformer.getClass())
						? new MethodInvokingTransformer(new LambdaMessageProcessor(genericTransformer, payloadType))
						: new MethodInvokingTransformer(genericTransformer, ClassUtils.TRANSFORMER_TRANSFORM_METHOD));
		return addComponent(transformer)
				.handle(new MessageTransformingHandler(transformer), endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MessageSelector} for the provided SpEL expression.
	 * @param expression the SpEL expression.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B filter(String expression) {
		return filter(expression, (Consumer<FilterEndpointSpec>) null);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MessageSelector} for the provided SpEL expression.
	 * In addition accept options for the integration endpoint using {@link FilterEndpointSpec}:
	 * <pre class="code">
	 * {@code
	 *  .filter("payload.hot"), e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param expression the SpEL expression.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see FilterEndpointSpec
	 */
	public B filter(String expression, Consumer<FilterEndpointSpec> endpointConfigurer) {
		Assert.hasText(expression, "'expression' must not be empty");
		return filter(null, new ExpressionEvaluatingSelector(expression), endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector} for the
	 * discovered method of the provided service.
	 * @param service the service to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingSelector
	 */
	public B filter(Object service) {
		return filter(service, null);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector} for the
	 * method of the provided service.
	 * @param service the service to use.
	 * @param methodName the method to invoke
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingSelector
	 */
	public B filter(Object service, String methodName) {
		return filter(service, methodName, null);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector} for the
	 * method of the provided service.
	 * @param service the service to use.
	 * @param methodName the method to invoke
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingSelector
	 */
	public B filter(Object service, String methodName, Consumer<FilterEndpointSpec> endpointConfigurer) {
		MethodInvokingSelector selector =
				StringUtils.hasText(methodName)
						? new MethodInvokingSelector(service, methodName)
						: new MethodInvokingSelector(service);
		return filter(null, selector, endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector}
	 * for the {@link MessageProcessor} from
	 * the provided {@link MessageProcessorSpec}.
	 * <pre class="code">
	 * {@code
	 *  .filter(Scripts.script(scriptResource).lang("ruby"))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B filter(MessageProcessorSpec<?> messageProcessorSpec) {
		return filter(messageProcessorSpec, (Consumer<FilterEndpointSpec>) null);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector}
	 * for the {@link MessageProcessor} from
	 * the provided {@link MessageProcessorSpec}.
	 * In addition accept options for the integration endpoint using {@link FilterEndpointSpec}.
	 * <pre class="code">
	 * {@code
	 *  .filter(Scripts.script(scriptResource).lang("ruby"),
	 *        e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B filter(MessageProcessorSpec<?> messageProcessorSpec, Consumer<FilterEndpointSpec> endpointConfigurer) {
		Assert.notNull(messageProcessorSpec, MESSAGE_PROCESSOR_SPEC_MUST_NOT_BE_NULL);
		MessageProcessor<?> processor = messageProcessorSpec.get();
		return addComponent(processor)
				.filter(null, new MethodInvokingSelector(processor), endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector}
	 * for the provided {@link GenericSelector}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .filter(Date.class, p -> p.after(new Date()))
	 * }
	 * </pre>
	 * @param payloadType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the selector.
	 * Conversion to this type will be attempted, if necessary.
	 * @param genericSelector the {@link GenericSelector} to use.
	 * @param <P> the source payload type or {@code Message.class}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P> B filter(Class<P> payloadType, GenericSelector<P> genericSelector) {
		return filter(payloadType, genericSelector, null);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector}
	 * for the provided {@link GenericSelector}.
	 * In addition accept options for the integration endpoint using {@link FilterEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .filter(Date.class, p -> p.after(new Date()), e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param payloadType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the selector.
	 * Conversion to this type will be attempted, if necessary.
	 * @param genericSelector the {@link GenericSelector} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the source payload type or {@code Message.class}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 * @see FilterEndpointSpec
	 */
	public <P> B filter(Class<P> payloadType, GenericSelector<P> genericSelector,
			Consumer<FilterEndpointSpec> endpointConfigurer) {

		Assert.notNull(genericSelector, "'genericSelector' must not be null");
		MessageSelector selector = genericSelector instanceof MessageSelector ? (MessageSelector) genericSelector :
				(ClassUtils.isLambda(genericSelector.getClass())
						? new MethodInvokingSelector(new LambdaMessageProcessor(genericSelector, payloadType))
						: new MethodInvokingSelector(genericSelector, ClassUtils.SELECTOR_ACCEPT_METHOD));
		return this.register(new FilterEndpointSpec(new MessageFilter(selector)), endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the selected protocol specific
	 * {@link MessageHandler} implementation from {@code Namespace Factory}:
	 * <pre class="code">
	 * {@code
	 *  .handle(Amqp.outboundAdapter(this.amqpTemplate).routingKeyExpression("headers.routingKey"))
	 * }
	 * </pre>
	 * @param messageHandlerSpec the {@link MessageHandlerSpec} to configure protocol specific
	 * {@link MessageHandler}.
	 * @param <H> the target {@link MessageHandler} type.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public <H extends MessageHandler> B handle(MessageHandlerSpec<?, H> messageHandlerSpec) {
		return handle(messageHandlerSpec, (Consumer<GenericEndpointSpec<H>>) null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the provided
	 * {@link MessageHandler} implementation.
	 * Can be used as Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handle(m -> logger.info(m.getPayload())
	 * }
	 * </pre>
	 * @param messageHandler the {@link MessageHandler} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B handle(MessageHandler messageHandler) {
		return handle(messageHandler, (Consumer<GenericEndpointSpec<MessageHandler>>) null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} at runtime.
	 * @param beanName the bean name to use.
	 * @param methodName the method to invoke.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B handle(String beanName, String methodName) {
		return handle(beanName, methodName, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param beanName the bean name to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B handle(String beanName, String methodName,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {
		return handle(new ServiceActivatingHandler(new BeanNameMessageProcessor<>(beanName, methodName)),
				endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the discovered {@code method} for provided {@code service} at runtime.
	 * @param service the service object to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B handle(Object service) {
		return handle(service, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param service the service object to use.
	 * @param methodName the method to invoke.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B handle(Object service, String methodName) {
		return handle(service, methodName, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param service the service object to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B handle(Object service, String methodName,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {

		ServiceActivatingHandler handler;
		if (StringUtils.hasText(methodName)) {
			handler = new ServiceActivatingHandler(service, methodName);
		}
		else {
			handler = new ServiceActivatingHandler(service);
		}
		return handle(handler, endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the provided {@link GenericHandler} at runtime.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handle(Integer.class, (p, h) -> p / 2)
	 * }
	 * </pre>
	 * @param payloadType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the handler.
	 * Conversion to this type will be attempted, if necessary.
	 * @param handler the handler to invoke.
	 * @param <P> the payload type to expect, or {@code Message.class}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P> B handle(Class<P> payloadType, GenericHandler<P> handler) {
		return handle(payloadType, handler, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the provided {@link GenericHandler} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handle(Integer.class, (p, h) -> p / 2, e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param payloadType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the handler.
	 * Conversion to this type will be attempted, if necessary.
	 * @param handler the handler to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type to expect or {@code Message.class}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P> B handle(Class<P> payloadType, GenericHandler<P> handler,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {

		ServiceActivatingHandler serviceActivatingHandler;
		if (ClassUtils.isLambda(handler.getClass())) {
			serviceActivatingHandler = new ServiceActivatingHandler(new LambdaMessageProcessor(handler, payloadType));
		}
		else {
			serviceActivatingHandler = new ServiceActivatingHandler(handler, ClassUtils.HANDLER_HANDLE_METHOD);
		}
		return handle(serviceActivatingHandler, endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link MessageProcessor} from the provided
	 *  {@link MessageProcessorSpec}.
	 * <pre class="code">
	 * {@code
	 *  .handle(Scripts.script("classpath:myScript.ruby"))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B handle(MessageProcessorSpec<?> messageProcessorSpec) {
		return handle(messageProcessorSpec, (Consumer<GenericEndpointSpec<ServiceActivatingHandler>>) null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link MessageProcessor} from the provided
	 *  {@link MessageProcessorSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * <pre class="code">
	 * {@code
	 *  .handle(Scripts.script("classpath:myScript.ruby"), e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B handle(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {

		Assert.notNull(messageProcessorSpec, MESSAGE_PROCESSOR_SPEC_MUST_NOT_BE_NULL);
		MessageProcessor<?> processor = messageProcessorSpec.get();
		return addComponent(processor)
				.handle(new ServiceActivatingHandler(processor), endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the selected protocol specific
	 * {@link MessageHandler} implementation from {@code Namespace Factory}:
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handle(Amqp.outboundAdapter(this.amqpTemplate).routingKeyExpression("headers.routingKey"),
	 *       e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param messageHandlerSpec the {@link MessageHandlerSpec} to configure protocol specific
	 * {@link MessageHandler}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <H> the {@link MessageHandler} type.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public <H extends MessageHandler> B handle(MessageHandlerSpec<?, H> messageHandlerSpec,
			Consumer<GenericEndpointSpec<H>> endpointConfigurer) {

		Assert.notNull(messageHandlerSpec, "'messageHandlerSpec' must not be null");
		if (messageHandlerSpec instanceof ComponentsRegistration) {
			addComponents(((ComponentsRegistration) messageHandlerSpec).getComponentsToRegister());
		}
		return handle(messageHandlerSpec.get(), endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the provided
	 * {@link MessageHandler} implementation.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Can be used as Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handle(m -> logger.info(m.getPayload()), e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param messageHandler the {@link MessageHandler} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <H> the {@link MessageHandler} type.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public <H extends MessageHandler> B handle(H messageHandler, Consumer<GenericEndpointSpec<H>> endpointConfigurer) {
		Assert.notNull(messageHandler, "'messageHandler' must not be null");
		return register(new GenericEndpointSpec<>(messageHandler), endpointConfigurer);
	}

	/**
	 * Populate a {@link BridgeHandler} to the current integration flow position.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #bridge(Consumer)
	 */
	public B bridge() {
		return bridge(null);
	}

	/**
	 * Populate a {@link BridgeHandler} to the current integration flow position.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .bridge(s -> s.poller(Pollers.fixedDelay(100))
	 *                   .autoStartup(false)
	 *                   .id("priorityChannelBridge"))
	 * }
	 * </pre>
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 */
	public B bridge(Consumer<GenericEndpointSpec<BridgeHandler>> endpointConfigurer) {
		return register(new GenericEndpointSpec<>(new BridgeHandler()), endpointConfigurer);
	}

	/**
	 * Populate a {@link DelayHandler} to the current integration flow position
	 * with default options.
	 * @param groupId the {@code groupId} for delayed messages in the
	 * {@link org.springframework.integration.store.MessageGroupStore}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B delay(String groupId) {
		return this.delay(groupId, null);
	}

	/**
	 * Populate a {@link DelayHandler} to the current integration flow position.
	 * @param groupId the {@code groupId} for delayed messages in the
	 * {@link org.springframework.integration.store.MessageGroupStore}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see DelayerEndpointSpec
	 */
	public B delay(String groupId, Consumer<DelayerEndpointSpec> endpointConfigurer) {
		return register(new DelayerEndpointSpec(new DelayHandler(groupId)), endpointConfigurer);
	}

	/**
	 * Populate a {@link org.springframework.integration.transformer.ContentEnricher}
	 * to the current integration flow position
	 * with provided options.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .enrich(e -> e.requestChannel("enrichChannel")
	 *                  .requestPayload(Message::getPayload)
	 *                  .shouldClonePayload(false)
	 *                  .autoStartup(false)
	 *                  .<Map<String, String>>headerFunction("foo", m -> m.getPayload().get("name")))
	 * }
	 * </pre>
	 * @param enricherConfigurer the {@link Consumer} to provide
	 * {@link org.springframework.integration.transformer.ContentEnricher} options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see EnricherSpec
	 */
	public B enrich(Consumer<EnricherSpec> enricherConfigurer) {
		return register(new EnricherSpec(), enricherConfigurer);
	}

	/**
	 * Populate a {@link MessageTransformingHandler} for
	 * a {@link org.springframework.integration.transformer.HeaderEnricher}
	 * using header values from provided {@link MapBuilder}.
	 * Can be used together with {@code Namespace Factory}:
	 * <pre class="code">
	 * {@code
	 *  .enrichHeaders(Mail.headers()
	 *                    .subjectFunction(m -> "foo")
	 *                    .from("foo@bar")
	 *                    .toFunction(m -> new String[] {"bar@baz"}))
	 * }
	 * </pre>
	 * @param headers the {@link MapBuilder} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B enrichHeaders(MapBuilder<?, String, Object> headers) {
		return enrichHeaders(headers, null);
	}

	/**
	 * Populate a {@link MessageTransformingHandler} for
	 * a {@link org.springframework.integration.transformer.HeaderEnricher}
	 * using header values from provided {@link MapBuilder}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Can be used together with {@code Namespace Factory}:
	 * <pre class="code">
	 * {@code
	 *  .enrichHeaders(Mail.headers()
	 *                    .subjectFunction(m -> "foo")
	 *                    .from("foo@bar")
	 *                    .toFunction(m -> new String[] {"bar@baz"}),
	 *                 e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param headers the {@link MapBuilder} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 */
	public B enrichHeaders(MapBuilder<?, String, Object> headers,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		return enrichHeaders(headers.get(), endpointConfigurer);
	}

	/**
	 * Accept a {@link Map} of values to be used for the
	 * {@link Message} header enrichment.
	 * {@code values} can apply an {@link Expression}
	 * to be evaluated against a request {@link Message}.
	 * @param headers the Map of headers to enrich.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B enrichHeaders(Map<String, Object> headers) {
		return enrichHeaders(headers, null);
	}

	/**
	 * Accept a {@link Map} of values to be used for the
	 * {@link Message} header enrichment.
	 * {@code values} can apply an {@link Expression}
	 * to be evaluated against a request {@link Message}.
	 * @param headers the Map of headers to enrich.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 */
	public B enrichHeaders(Map<String, Object> headers,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		HeaderEnricherSpec headerEnricherSpec = new HeaderEnricherSpec();
		headerEnricherSpec.headers(headers);
		Tuple2<ConsumerEndpointFactoryBean, MessageTransformingHandler> tuple2 = headerEnricherSpec.get();
		return addComponents(headerEnricherSpec.getComponentsToRegister())
				.handle(tuple2.getT2(), endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageTransformingHandler} for
	 * a {@link org.springframework.integration.transformer.HeaderEnricher}
	 * as the result of provided {@link Consumer}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .enrichHeaders(h -> h.header(FileHeaders.FILENAME, "foo.sitest")
	 *                       .header("directory", new File(tmpDir, "fileWritingFlow")))
	 * }
	 * </pre>
	 * @param headerEnricherConfigurer the {@link Consumer} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see HeaderEnricherSpec
	 */
	public B enrichHeaders(Consumer<HeaderEnricherSpec> headerEnricherConfigurer) {
		Assert.notNull(headerEnricherConfigurer, "'headerEnricherConfigurer' must not be null");
		return register(new HeaderEnricherSpec(), headerEnricherConfigurer);
	}

	/**
	 * Populate the {@link DefaultMessageSplitter} with default options
	 * to the current integration flow position.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B split() {
		return split((Consumer<SplitterEndpointSpec<DefaultMessageSplitter>>) null);
	}

	/**
	 * Populate the {@link DefaultMessageSplitter} with provided options
	 * to the current integration flow position.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .split(s -> s.applySequence(false).delimiters(","))
	 * }
	 * </pre>
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link DefaultMessageSplitter}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public B split(Consumer<SplitterEndpointSpec<DefaultMessageSplitter>> endpointConfigurer) {
		return split(new DefaultMessageSplitter(), endpointConfigurer);
	}

	/**
	 * Populate the {@link ExpressionEvaluatingSplitter} with provided
	 * SpEL expression.
	 * @param expression the splitter SpEL expression.
	 * and for {@link ExpressionEvaluatingSplitter}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public B split(String expression) {
		return split(expression, (Consumer<SplitterEndpointSpec<ExpressionEvaluatingSplitter>>) null);
	}

	/**
	 * Populate the {@link ExpressionEvaluatingSplitter} with provided
	 * SpEL expression.
	 * @param expression the splitter SpEL expression.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link ExpressionEvaluatingSplitter}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public B split(String expression, Consumer<SplitterEndpointSpec<ExpressionEvaluatingSplitter>> endpointConfigurer) {
		Assert.hasText(expression, "'expression' must not be empty");
		return split(new ExpressionEvaluatingSplitter(PARSER.parseExpression(expression)), endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the discovered
	 * {@code method} of the {@code service} at runtime.
	 * @param service the service to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingSplitter
	 */
	public B split(Object service) {
		return split(service, null);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@code method} of the {@code service} at runtime.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingSplitter
	 */
	public B split(Object service, String methodName) {
		return split(service, methodName, null);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@code method} of the {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link MethodInvokingSplitter}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 * @see MethodInvokingSplitter
	 */
	public B split(Object service, String methodName,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) {

		MethodInvokingSplitter splitter;
		if (StringUtils.hasText(methodName)) {
			splitter = new MethodInvokingSplitter(service, methodName);
		}
		else {
			splitter = new MethodInvokingSplitter(service);
		}
		return split(splitter, endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@code method} of the {@code bean} at runtime.
	 * @param beanName the bean name to use.
	 * @param methodName the method to invoke at runtime.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B split(String beanName, String methodName) {
		return split(beanName, methodName, null);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@code method} of the {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param beanName the bean name to use.
	 * @param methodName the method to invoke at runtime.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link MethodInvokingSplitter}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public B split(String beanName, String methodName,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) {

		return split(new MethodInvokingSplitter(new BeanNameMessageProcessor<>(beanName, methodName)),
				endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the
	 * {@link MessageProcessor} at runtime
	 * from provided {@link MessageProcessorSpec}.
	 * <pre class="code">
	 * {@code
	 *  .split(Scripts.script("classpath:myScript.ruby"))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the splitter {@link MessageProcessorSpec}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public B split(MessageProcessorSpec<?> messageProcessorSpec) {
		return split(messageProcessorSpec, (Consumer<SplitterEndpointSpec<MethodInvokingSplitter>>) null);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the
	 * {@link MessageProcessor} at runtime
	 * from provided {@link MessageProcessorSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * <pre class="code">
	 * {@code
	 *  .split(Scripts.script(myScriptResource).lang("groovy").refreshCheckDelay(1000),
	 *  			, e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the splitter {@link MessageProcessorSpec}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link MethodInvokingSplitter}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public B split(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) {

		Assert.notNull(messageProcessorSpec, MESSAGE_PROCESSOR_SPEC_MUST_NOT_BE_NULL);
		MessageProcessor<?> processor = messageProcessorSpec.get();
		return addComponent(processor)
				.split(new MethodInvokingSplitter(processor), endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@link Function} at runtime.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .split(String.class, p ->
	 *        jdbcTemplate.execute("SELECT * from FOO",
	 *            (PreparedStatement ps) ->
	 *                 new ResultSetIterator<Foo>(ps.executeQuery(),
	 *                     (rs, rowNum) ->
	 *                           new Foo(rs.getInt(1), rs.getString(2)))))
	 * }
	 * </pre>
	 * @param payloadType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the splitter.
	 * Conversion to this type will be attempted, if necessary.
	 * @param splitter the splitter {@link Function}.
	 * @param <P> the payload type or {@code Message.class}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P> B split(Class<P> payloadType, Function<P, ?> splitter) {
		return split(payloadType, splitter, null);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@link Function} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .split(String.class, p ->
	 *        jdbcTemplate.execute("SELECT * from FOO",
	 *            (PreparedStatement ps) ->
	 *                 new ResultSetIterator<Foo>(ps.executeQuery(),
	 *                     (rs, rowNum) ->
	 *                           new Foo(rs.getInt(1), rs.getString(2))))
	 *       , e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param payloadType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the splitter.
	 * Conversion to this type will be attempted, if necessary.
	 * @param splitter the splitter {@link Function}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type or {@code Message.class}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 * @see SplitterEndpointSpec
	 */
	public <P> B split(Class<P> payloadType, Function<P, ?> splitter,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) {

		MethodInvokingSplitter split =
				ClassUtils.isLambda(splitter.getClass())
						? new MethodInvokingSplitter(new LambdaMessageProcessor(splitter, payloadType))
						: new MethodInvokingSplitter(splitter, ClassUtils.FUNCTION_APPLY_METHOD);
		return split(split, endpointConfigurer);
	}

	/**
	 * Populate the provided {@link AbstractMessageSplitter} to the current integration
	 * flow position.
	 * @param splitterMessageHandlerSpec the {@link MessageHandlerSpec} to populate.
	 * @param <S> the {@link AbstractMessageSplitter}
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public <S extends AbstractMessageSplitter> B split(MessageHandlerSpec<?, S> splitterMessageHandlerSpec) {
		return split(splitterMessageHandlerSpec, (Consumer<SplitterEndpointSpec<S>>) null);
	}

	/**
	 * Populate the provided {@link AbstractMessageSplitter} to the current integration
	 * flow position.
	 * @param splitterMessageHandlerSpec the {@link MessageHandlerSpec} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <S> the {@link AbstractMessageSplitter}
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public <S extends AbstractMessageSplitter> B split(MessageHandlerSpec<?, S> splitterMessageHandlerSpec,
			Consumer<SplitterEndpointSpec<S>> endpointConfigurer) {
		Assert.notNull(splitterMessageHandlerSpec, "'splitterMessageHandlerSpec' must not be null");
		return split(splitterMessageHandlerSpec.get(), endpointConfigurer);
	}

	/**
	 * Populate the provided {@link AbstractMessageSplitter} to the current integration
	 * flow position.
	 * @param splitter the {@link AbstractMessageSplitter} to populate.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public B split(AbstractMessageSplitter splitter) {
		return split(splitter, (Consumer<SplitterEndpointSpec<AbstractMessageSplitter>>) null);
	}

	/**
	 * Populate the provided {@link AbstractMessageSplitter} to the current integration
	 * flow position.
	 * @param splitter the {@link AbstractMessageSplitter} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <S> the {@link AbstractMessageSplitter}
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public <S extends AbstractMessageSplitter> B split(S splitter,
			Consumer<SplitterEndpointSpec<S>> endpointConfigurer) {

		Assert.notNull(splitter, "'splitter' must not be null");
		return register(new SplitterEndpointSpec<>(splitter), endpointConfigurer);
	}

	/**
	 * Provide the {@link HeaderFilter} to the current {@link StandardIntegrationFlow}.
	 * @param headersToRemove the array of headers (or patterns)
	 * to remove from {@link org.springframework.messaging.MessageHeaders}.
	 * @return this {@link BaseIntegrationFlowDefinition}.
	 */
	public B headerFilter(String... headersToRemove) {
		return headerFilter(new HeaderFilter(headersToRemove), null);
	}

	/**
	 * Provide the {@link HeaderFilter} to the current {@link StandardIntegrationFlow}.
	 * @param headersToRemove the comma separated headers (or patterns) to remove from
	 * {@link org.springframework.messaging.MessageHeaders}.
	 * @param patternMatch the {@code boolean} flag to indicate if {@code headersToRemove}
	 * should be interpreted as patterns or direct header names.
	 * @return this {@link BaseIntegrationFlowDefinition}.
	 */
	public B headerFilter(String headersToRemove, boolean patternMatch) {
		HeaderFilter headerFilter = new HeaderFilter(StringUtils.delimitedListToStringArray(headersToRemove, ",", " "));
		headerFilter.setPatternMatch(patternMatch);
		return headerFilter(headerFilter, null);
	}

	/**
	 * Populate the provided {@link MessageTransformingHandler} for the provided
	 * {@link HeaderFilter}.
	 * @param headerFilter the {@link HeaderFilter} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 */
	public B headerFilter(HeaderFilter headerFilter,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		return transform(null, headerFilter, endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the {@link ClaimCheckInTransformer}
	 * with provided {@link MessageStore}.
	 * @param messageStore the {@link MessageStore} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B claimCheckIn(MessageStore messageStore) {
		return this.claimCheckIn(messageStore, null);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the {@link ClaimCheckInTransformer}
	 * with provided {@link MessageStore}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param messageStore the {@link MessageStore} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 */
	public B claimCheckIn(MessageStore messageStore,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		return transform(null, new ClaimCheckInTransformer(messageStore), endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the {@link ClaimCheckOutTransformer}
	 * with provided {@link MessageStore}.
	 * The {@code removeMessage} option of {@link ClaimCheckOutTransformer} is to {@code false}.
	 * @param messageStore the {@link MessageStore} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B claimCheckOut(MessageStore messageStore) {
		return claimCheckOut(messageStore, false);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the {@link ClaimCheckOutTransformer}
	 * with provided {@link MessageStore} and {@code removeMessage} flag.
	 * @param messageStore the {@link MessageStore} to use.
	 * @param removeMessage the removeMessage boolean flag.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see ClaimCheckOutTransformer#setRemoveMessage(boolean)
	 */
	public B claimCheckOut(MessageStore messageStore, boolean removeMessage) {
		return claimCheckOut(messageStore, removeMessage, null);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the {@link ClaimCheckOutTransformer}
	 * with provided {@link MessageStore} and {@code removeMessage} flag.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param messageStore the {@link MessageStore} to use.
	 * @param removeMessage the removeMessage boolean flag.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 * @see ClaimCheckOutTransformer#setRemoveMessage(boolean)
	 */
	public B claimCheckOut(MessageStore messageStore, boolean removeMessage,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		ClaimCheckOutTransformer claimCheckOutTransformer = new ClaimCheckOutTransformer(messageStore);
		claimCheckOutTransformer.setRemoveMessage(removeMessage);
		return transform(null, claimCheckOutTransformer, endpointConfigurer);
	}

	/**
	 * Populate the
	 * {@link org.springframework.integration.aggregator.ResequencingMessageHandler} with
	 * default options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B resequence() {
		return resequence(null);
	}

	/**
	 * Populate the
	 * {@link org.springframework.integration.aggregator.ResequencingMessageHandler} with
	 * provided options from {@link ResequencerSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .resequence(r -> r.releasePartialSequences(true)
	 *                    .correlationExpression("'foo'")
	 *                    .phase(100))
	 * }
	 * </pre>
	 * @param resequencer the {@link Consumer} to provide
	 * {@link org.springframework.integration.aggregator.ResequencingMessageHandler} options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see ResequencerSpec
	 */
	public B resequence(Consumer<ResequencerSpec> resequencer) {
		return register(new ResequencerSpec(), resequencer);
	}

	/**
	 * Populate the {@link AggregatingMessageHandler} with default options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B aggregate() {
		return aggregate(null);
	}

	/**
	 * Populate the {@link AggregatingMessageHandler} with provided options from {@link AggregatorSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .aggregate(a -> a.correlationExpression("1")
	 *                   .releaseStrategy(g -> g.size() == 25)
	 *                   .phase(100))
	 * }
	 * </pre>
	 * @param aggregator the {@link Consumer} to provide {@link AggregatingMessageHandler} options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see AggregatorSpec
	 */
	public B aggregate(Consumer<AggregatorSpec> aggregator) {
		return register(new AggregatorSpec(), aggregator);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided bean and its method
	 * with default options.
	 * @param beanName the bean to use.
	 * @param method the method to invoke at runtime.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B route(String beanName, String method) {
		return route(beanName, method, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided bean and its method
	 * with provided options from {@link RouterSpec}.
	 * @param beanName the bean to use.
	 * @param method the method to invoke at runtime.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B route(String beanName, String method, Consumer<RouterSpec<Object,
			MethodInvokingRouter>> routerConfigurer) {

		MethodInvokingRouter methodInvokingRouter =
				new MethodInvokingRouter(new BeanNameMessageProcessor<>(beanName, method));
		return route(new RouterSpec<>(methodInvokingRouter), routerConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the discovered method
	 * of the provided service and its method with default options.
	 * @param service the bean to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingRouter
	 */
	public B route(Object service) {
		return route(service, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the method
	 * of the provided service and its method with default options.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingRouter
	 */
	public B route(Object service, String methodName) {
		return route(service, methodName, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the method
	 * of the provided service and its method with provided options from {@link RouterSpec}.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see MethodInvokingRouter
	 */
	public B route(Object service, String methodName,
			Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer) {

		MethodInvokingRouter router;
		if (StringUtils.hasText(methodName)) {
			router = new MethodInvokingRouter(service, methodName);
		}
		else {
			router = new MethodInvokingRouter(service);
		}
		return route(new RouterSpec<>(router), routerConfigurer);
	}

	/**
	 * Populate the {@link ExpressionEvaluatingRouter} for provided SpEL expression
	 * with default options.
	 * @param expression the expression to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B route(String expression) {
		return route(expression, (Consumer<RouterSpec<Object, ExpressionEvaluatingRouter>>) null);
	}

	/**
	 * Populate the {@link ExpressionEvaluatingRouter} for provided SpEL expression
	 * with provided options from {@link RouterSpec}.
	 * @param expression the expression to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link ExpressionEvaluatingRouter} options.
	 * @param <T> the target result type.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public <T> B route(String expression, Consumer<RouterSpec<T, ExpressionEvaluatingRouter>> routerConfigurer) {
		return route(new RouterSpec<>(new ExpressionEvaluatingRouter(PARSER.parseExpression(expression))),
				routerConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * and payload type with default options.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .route(Integer.class, p -> p % 2 == 0)
	 * }
	 * </pre>
	 * @param payloadType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the splitter.
	 * Conversion to this type will be attempted, if necessary.
	 * @param router  the {@link Function} to use.
	 * @param <S> the source payload type or {@code Message.class}.
	 * @param <T> the target result type.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <S, T> B route(Class<S> payloadType, Function<S, T> router) {
		return route(payloadType, router, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * and payload type and options from {@link RouterSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .route(Integer.class, p -> p % 2 == 0,
	 *					m -> m.channelMapping("true", "evenChannel")
	 *                       .subFlowMapping("false", f ->
	 *                                   f.<Integer>handle((p, h) -> p * 3))
	 *                       .applySequence(false))
	 * }
	 * </pre>
	 * @param payloadType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the splitter.
	 * Conversion to this type will be attempted, if necessary.
	 * @param router the {@link Function} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param <P> the source payload type or {@code Message.class}.
	 * @param <T> the target result type.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P, T> B route(Class<P> payloadType, Function<P, T> router,
			Consumer<RouterSpec<T, MethodInvokingRouter>> routerConfigurer) {

		MethodInvokingRouter methodInvokingRouter =
				ClassUtils.isLambda(router.getClass())
						? new MethodInvokingRouter(new LambdaMessageProcessor(router, payloadType))
						: new MethodInvokingRouter(router, ClassUtils.FUNCTION_APPLY_METHOD);
		return route(new RouterSpec<>(methodInvokingRouter), routerConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the
	 * {@link MessageProcessor}
	 * from the provided {@link MessageProcessorSpec} with default options.
	 * <pre class="code">
	 * {@code
	 *  .route(Scripts.script(myScriptResource).lang("groovy").refreshCheckDelay(1000))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B route(MessageProcessorSpec<?> messageProcessorSpec) {
		return route(messageProcessorSpec, (Consumer<RouterSpec<Object, MethodInvokingRouter>>) null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the
	 * {@link MessageProcessor}
	 * from the provided {@link MessageProcessorSpec} with default options.
	 * <pre class="code">
	 * {@code
	 *  .route(Scripts.script(myScriptResource).lang("groovy").refreshCheckDelay(1000),
	 *                 m -> m.channelMapping("true", "evenChannel")
	 *                       .subFlowMapping("false", f ->
	 *                                   f.<Integer>handle((p, h) -> p * 3)))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B route(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer) {

		Assert.notNull(messageProcessorSpec, MESSAGE_PROCESSOR_SPEC_MUST_NOT_BE_NULL);
		MessageProcessor<?> processor = messageProcessorSpec.get();
		addComponent(processor);

		return route(new RouterSpec<>(new MethodInvokingRouter(processor)), routerConfigurer);
	}

	protected <R extends AbstractMessageRouter, S extends AbstractRouterSpec<? super S, R>> B route(S routerSpec,
			Consumer<S> routerConfigurer) {

		if (routerConfigurer != null) {
			routerConfigurer.accept(routerSpec);
		}

		BridgeHandler bridgeHandler = new BridgeHandler();
		boolean registerSubflowBridge = false;

		Map<Object, String> componentsToRegister = null;
		Map<Object, String> routerComponents = routerSpec.getComponentsToRegister();
		if (routerComponents != null) {
			componentsToRegister = new LinkedHashMap<>(routerComponents);
			routerComponents.clear();
		}

		register(routerSpec, null);

		if (!CollectionUtils.isEmpty(componentsToRegister)) {
			for (Map.Entry<Object, String> entry : componentsToRegister.entrySet()) {
				Object component = entry.getKey();
				if (component instanceof BaseIntegrationFlowDefinition) {
					BaseIntegrationFlowDefinition<?> flowBuilder = (BaseIntegrationFlowDefinition<?>) component;
					if (flowBuilder.isOutputChannelRequired()) {
						registerSubflowBridge = true;
						flowBuilder.channel(new FixedSubscriberChannel(bridgeHandler));
					}
					addComponent(flowBuilder.get());
				}
				else {
					addComponent(component, entry.getValue());
				}
			}
		}

		if (routerSpec.isDefaultToParentFlow()) {
			routerSpec.defaultOutputChannel(new FixedSubscriberChannel(bridgeHandler));
			registerSubflowBridge = true;
		}

		if (registerSubflowBridge) {
			currentComponent(null)
					.handle(bridgeHandler);
		}
		return _this();
	}

	/**
	 * Populate the {@link RecipientListRouter} with options from the {@link RecipientListRouterSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .routeToRecipients(r -> r
	 *      .recipient("bar-channel", m ->
	 *            m.getHeaders().containsKey("recipient") && (boolean) m.getHeaders().get("recipient"))
	 *      .recipientFlow("'foo' == payload or 'bar' == payload or 'baz' == payload",
	 *                         f -> f.transform(String.class, p -> p.toUpperCase())
	 *                               .channel(c -> c.queue("recipientListSubFlow1Result"))))
	 * }
	 * </pre>
	 * @param routerConfigurer the {@link Consumer} to provide {@link RecipientListRouter} options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B routeToRecipients(Consumer<RecipientListRouterSpec> routerConfigurer) {
		return route(new RecipientListRouterSpec(), routerConfigurer);
	}

	/**
	 * Populate the {@link ErrorMessageExceptionTypeRouter} with options from the {@link RouterSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .routeByException(r -> r
	 *      .channelMapping(IllegalArgumentException.class, "illegalArgumentChannel")
	 *      .subFlowMapping(MessageHandlingException.class, sf ->
	 *                                sf.handle(...))
	 *    )
	 * }
	 * </pre>
	 * @param routerConfigurer the {@link Consumer} to provide {@link ErrorMessageExceptionTypeRouter} options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see ErrorMessageExceptionTypeRouter
	 */
	public B routeByException(
			Consumer<RouterSpec<Class<? extends Throwable>, ErrorMessageExceptionTypeRouter>> routerConfigurer) {
		return route(new RouterSpec<>(new ErrorMessageExceptionTypeRouter()), routerConfigurer);
	}

	/**
	 * Populate the provided {@link AbstractMessageRouter} implementation to the
	 * current integration flow position.
	 * @param router the {@link AbstractMessageRouter} to populate.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B route(AbstractMessageRouter router) {
		return route(router, (Consumer<GenericEndpointSpec<AbstractMessageRouter>>) null);
	}

	/**
	 * Populate the provided {@link AbstractMessageRouter} implementation to the
	 * current integration flow position.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param router the {@link AbstractMessageRouter} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <R> the {@link AbstractMessageRouter} type.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public <R extends AbstractMessageRouter> B route(R router, Consumer<GenericEndpointSpec<R>> endpointConfigurer) {
		return handle(router, endpointConfigurer);
	}

	/**
	 * Populate the "artificial"
	 * {@link org.springframework.integration.gateway.GatewayMessageHandler} for the
	 * provided {@code requestChannel} to send a request with default options.
	 * Uses {@link org.springframework.integration.gateway.RequestReplyExchanger} Proxy
	 * on the background.
	 * @param requestChannel the {@link MessageChannel} bean name.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B gateway(String requestChannel) {
		return gateway(requestChannel, null);
	}

	/**
	 * Populate the "artificial"
	 * {@link org.springframework.integration.gateway.GatewayMessageHandler} for the
	 * provided {@code requestChannel} to send a request with options from
	 * {@link GatewayEndpointSpec}. Uses
	 * {@link org.springframework.integration.gateway.RequestReplyExchanger} Proxy on the
	 * background.
	 * @param requestChannel the {@link MessageChannel} bean name.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint
	 * options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B gateway(String requestChannel, Consumer<GatewayEndpointSpec> endpointConfigurer) {
		return register(new GatewayEndpointSpec(requestChannel), endpointConfigurer);
	}

	/**
	 * Populate the "artificial"
	 * {@link org.springframework.integration.gateway.GatewayMessageHandler}
	 * for the provided {@code requestChannel} to send a request with default options.
	 * Uses {@link org.springframework.integration.gateway.RequestReplyExchanger} Proxy on
	 * the background.
	 * @param requestChannel the {@link MessageChannel} to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B gateway(MessageChannel requestChannel) {
		return gateway(requestChannel, null);
	}

	/**
	 * Populate the "artificial"
	 * {@link org.springframework.integration.gateway.GatewayMessageHandler} for the
	 * provided {@code requestChannel} to send a request with options from
	 * {@link GatewayEndpointSpec}. Uses
	 * {@link org.springframework.integration.gateway.RequestReplyExchanger} Proxy on the
	 * background.
	 * @param requestChannel the {@link MessageChannel} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint
	 * options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B gateway(MessageChannel requestChannel, Consumer<GatewayEndpointSpec> endpointConfigurer) {
		return register(new GatewayEndpointSpec(requestChannel), endpointConfigurer);
	}

	/**
	 * Populate the "artificial"
	 * {@link org.springframework.integration.gateway.GatewayMessageHandler} for the
	 * provided {@code subflow}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .gateway(f -> f.transform("From Gateway SubFlow: "::concat))
	 * }
	 * </pre>
	 * @param flow the {@link IntegrationFlow} to to send a request message and wait for reply.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B gateway(IntegrationFlow flow) {
		return gateway(flow, null);
	}

	/**
	 * Populate the "artificial"
	 * {@link org.springframework.integration.gateway.GatewayMessageHandler} for the
	 * provided {@code subflow} with options from {@link GatewayEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .gateway(f -> f.transform("From Gateway SubFlow: "::concat), e -> e.replyTimeout(100L))
	 * }
	 * </pre>
	 * @param flow the {@link IntegrationFlow} to to send a request message and wait for reply.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B gateway(IntegrationFlow flow, Consumer<GatewayEndpointSpec> endpointConfigurer) {
		MessageChannel requestChannel = obtainInputChannelFromFlow(flow);
		return gateway(requestChannel, endpointConfigurer);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO}
	 * logging level and {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category.
	 * <p> The full request {@link Message} will be logged.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public B log() {
		return log(LoggingHandler.Level.INFO);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for provided {@link LoggingHandler.Level}
	 * logging level and {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category.
	 * <p> The full request {@link Message} will be logged.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param level the {@link LoggingHandler.Level}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public B log(LoggingHandler.Level level) {
		return log(level, (String) null);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided logging category
	 * and {@code INFO} logging level.
	 * <p> The full request {@link Message} will be logged.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param category the logging category to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public B log(String category) {
		return log(LoggingHandler.Level.INFO, category);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level and logging category.
	 * <p> The full request {@link Message} will be logged.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category to use.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public B log(LoggingHandler.Level level, String category) {
		return log(level, category, (Expression) null);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level, logging category
	 * and SpEL expression for the log message.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category.
	 * @param logExpression the SpEL expression to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public B log(LoggingHandler.Level level, String category, String logExpression) {
		Assert.hasText(logExpression, "'logExpression' must not be empty");
		return log(level, category, PARSER.parseExpression(logExpression));
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and {@link Function} for the log message.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public <P> B log(Function<Message<P>, Object> function) {
		Assert.notNull(function, FUNCTION_MUST_NOT_BE_NULL);
		return log(new FunctionExpression<>(function));
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and SpEL expression to evaluate
	 * logger message at runtime against the request {@link Message}.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public B log(Expression logExpression) {
		return log(LoggingHandler.Level.INFO, logExpression);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and SpEL expression to evaluate
	 * logger message at runtime against the request {@link Message}.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public B log(LoggingHandler.Level level, Expression logExpression) {
		return log(level, null, logExpression);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO}
	 * {@link LoggingHandler.Level} logging level,
	 * the provided logging category and SpEL expression to evaluate
	 * logger message at runtime against the request {@link Message}.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param category the logging category.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public B log(String category, Expression logExpression) {
		return log(LoggingHandler.Level.INFO, category, logExpression);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and {@link Function} for the log message.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public <P> B log(LoggingHandler.Level level, Function<Message<P>, Object> function) {
		return log(level, null, function);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level,
	 * the provided logging category and {@link Function} for the log message.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param category the logging category.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public <P> B log(String category, Function<Message<P>, Object> function) {
		return log(LoggingHandler.Level.INFO, category, function);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level, logging category
	 * and {@link Function} for the log message.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public <P> B log(LoggingHandler.Level level, String category, Function<Message<P>, Object> function) {
		Assert.notNull(function, FUNCTION_MUST_NOT_BE_NULL);
		return log(level, category, new FunctionExpression<>(function));
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level, logging category
	 * and SpEL expression for the log message.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * The {@link #logAndReply()} should be used for request-reply configuration.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @see #wireTap(WireTapSpec)
	 */
	public B log(LoggingHandler.Level level, String category, Expression logExpression) {
		LoggingHandler loggingHandler = new LoggingHandler(level);
		if (StringUtils.hasText(category)) {
			loggingHandler.setLoggerName(category);
		}

		if (logExpression != null) {
			loggingHandler.setLogExpression(logExpression);
		}
		else {
			loggingHandler.setShouldLogFullMessage(true);
		}

		addComponent(loggingHandler);
		MessageChannel loggerChannel = new FixedSubscriberChannel(loggingHandler);
		return wireTap(loggerChannel);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO}
	 * logging level and {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category.
	 * <p> The full request {@link Message} will be logged.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public IntegrationFlow logAndReply() {
		return logAndReply(LoggingHandler.Level.INFO);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for provided {@link LoggingHandler.Level}
	 * logging level and {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category.
	 * <p> The full request {@link Message} will be logged.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param level the {@link LoggingHandler.Level}.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public IntegrationFlow logAndReply(LoggingHandler.Level level) {
		return logAndReply(level, (String) null);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided logging category
	 * and {@code INFO} logging level.
	 * <p> The full request {@link Message} will be logged.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param category the logging category to use.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public IntegrationFlow logAndReply(String category) {
		return logAndReply(LoggingHandler.Level.INFO, category);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level and logging category.
	 * <p> The full request {@link Message} will be logged.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category to use.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public IntegrationFlow logAndReply(LoggingHandler.Level level, String category) {
		return logAndReply(level, category, (Expression) null);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level, logging category
	 * and SpEL expression for the log message.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category.
	 * @param logExpression the SpEL expression to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public IntegrationFlow logAndReply(LoggingHandler.Level level, String category, String logExpression) {
		Assert.hasText(logExpression, "'logExpression' must not be empty");
		return logAndReply(level, category, PARSER.parseExpression(logExpression));
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and {@link Function} for the log message.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public <P> IntegrationFlow logAndReply(Function<Message<P>, Object> function) {
		Assert.notNull(function, FUNCTION_MUST_NOT_BE_NULL);
		return logAndReply(new FunctionExpression<>(function));
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and SpEL expression to evaluate
	 * logger message at runtime against the request {@link Message}.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public IntegrationFlow logAndReply(Expression logExpression) {
		return logAndReply(LoggingHandler.Level.INFO, logExpression);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and SpEL expression to evaluate
	 * logger message at runtime against the request {@link Message}.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public IntegrationFlow logAndReply(LoggingHandler.Level level, Expression logExpression) {
		return logAndReply(level, null, logExpression);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO}
	 * {@link LoggingHandler.Level} logging level,
	 * the provided logging category and SpEL expression to evaluate
	 * logger message at runtime against the request {@link Message}.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param category the logging category.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public IntegrationFlow logAndReply(String category, Expression logExpression) {
		return logAndReply(LoggingHandler.Level.INFO, category, logExpression);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and {@link Function} for the log message.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public <P> IntegrationFlow logAndReply(LoggingHandler.Level level, Function<Message<P>, Object> function) {
		return logAndReply(level, null, function);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level,
	 * the provided logging category and {@link Function} for the log message.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param category the logging category.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public <P> IntegrationFlow logAndReply(String category, Function<Message<P>, Object> function) {
		return logAndReply(LoggingHandler.Level.INFO, category, function);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level, logging category
	 * and {@link Function} for the log message.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public <P> IntegrationFlow logAndReply(LoggingHandler.Level level, String category,
			Function<Message<P>, Object> function) {

		Assert.notNull(function, FUNCTION_MUST_NOT_BE_NULL);
		return logAndReply(level, category, new FunctionExpression<>(function));
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level, logging category
	 * and SpEL expression for the log message.
	 * <p> A {@link #bridge()} is added after this operator to make the flow reply-producing
	 * if the {@code replyChannel} header is present.
	 * <p> This operator can be used only in the end of flow.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return an {@link IntegrationFlow} instance based on this builder.
	 * @see #log()
	 * @see #bridge()
	 */
	public IntegrationFlow logAndReply(LoggingHandler.Level level, String category, Expression logExpression) {
		return log(level, category, logExpression)
				.bridge()
				.get();
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link MessageChannel} for scattering function
	 * and default {@link AggregatorSpec} for gathering function.
	 * @param scatterChannel the {@link MessageChannel} for scatting requests.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B scatterGather(MessageChannel scatterChannel) {
		return scatterGather(scatterChannel, null);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link MessageChannel} for scattering function
	 * and {@link AggregatorSpec} for gathering function.
	 * @param scatterChannel the {@link MessageChannel} for scatting requests.
	 * @param gatherer the {@link Consumer} for {@link AggregatorSpec} to configure gatherer.
	 * Can be {@code null}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B scatterGather(MessageChannel scatterChannel, Consumer<AggregatorSpec> gatherer) {
		return scatterGather(scatterChannel, gatherer, null);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link MessageChannel} for scattering function
	 * and {@link AggregatorSpec} for gathering function.
	 * @param scatterChannel the {@link MessageChannel} for scatting requests.
	 * @param gatherer the {@link Consumer} for {@link AggregatorSpec} to configure gatherer.
	 * Can be {@code null}.
	 * @param scatterGather the {@link Consumer} for {@link ScatterGatherSpec} to configure
	 * {@link ScatterGatherHandler} and its endpoint. Can be {@code null}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B scatterGather(MessageChannel scatterChannel, Consumer<AggregatorSpec> gatherer,
			Consumer<ScatterGatherSpec> scatterGather) {

		AggregatorSpec aggregatorSpec = new AggregatorSpec();
		if (gatherer != null) {
			gatherer.accept(aggregatorSpec);
		}
		AggregatingMessageHandler aggregatingMessageHandler = aggregatorSpec.get().getT2();
		addComponent(aggregatingMessageHandler);
		ScatterGatherHandler messageHandler = new ScatterGatherHandler(scatterChannel, aggregatingMessageHandler);
		return register(new ScatterGatherSpec(messageHandler), scatterGather);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link RecipientListRouterSpec} for scattering function
	 * and default {@link AggregatorSpec} for gathering function.
	 * @param scatterer the {@link Consumer} for {@link RecipientListRouterSpec} to configure scatterer.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B scatterGather(Consumer<RecipientListRouterSpec> scatterer) {
		return scatterGather(scatterer, null);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link RecipientListRouterSpec} for scattering function
	 * and {@link AggregatorSpec} for gathering function.
	 * @param scatterer the {@link Consumer} for {@link RecipientListRouterSpec} to configure scatterer.
	 * Can be {@code null}.
	 * @param gatherer the {@link Consumer} for {@link AggregatorSpec} to configure gatherer.
	 * Can be {@code null}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B scatterGather(Consumer<RecipientListRouterSpec> scatterer, @Nullable Consumer<AggregatorSpec> gatherer) {
		return scatterGather(scatterer, gatherer, null);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link RecipientListRouterSpec} for scattering function
	 * and {@link AggregatorSpec} for gathering function.
	 * @param scatterer the {@link Consumer} for {@link RecipientListRouterSpec} to configure scatterer.
	 * @param gatherer the {@link Consumer} for {@link AggregatorSpec} to configure gatherer.
	 * @param scatterGather the {@link Consumer} for {@link ScatterGatherSpec} to configure
	 * {@link ScatterGatherHandler} and its endpoint. Can be {@code null}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B scatterGather(Consumer<RecipientListRouterSpec> scatterer, @Nullable Consumer<AggregatorSpec> gatherer,
			@Nullable Consumer<ScatterGatherSpec> scatterGather) {

		Assert.notNull(scatterer, "'scatterer' must not be null");
		RecipientListRouterSpec recipientListRouterSpec = new RecipientListRouterSpec();
		scatterer.accept(recipientListRouterSpec);
		AggregatorSpec aggregatorSpec = new AggregatorSpec();
		if (gatherer != null) {
			gatherer.accept(aggregatorSpec);
		}
		RecipientListRouter recipientListRouter = recipientListRouterSpec.get().getT2();
		addComponent(recipientListRouter)
				.addComponents(recipientListRouterSpec.getComponentsToRegister());
		AggregatingMessageHandler aggregatingMessageHandler = aggregatorSpec.get().getT2();
		addComponent(aggregatingMessageHandler);
		ScatterGatherHandler messageHandler = new ScatterGatherHandler(recipientListRouter, aggregatingMessageHandler);
		return register(new ScatterGatherSpec(messageHandler), scatterGather);
	}

	/**
	 * Populate a {@link org.springframework.integration.aggregator.BarrierMessageHandler}
	 * instance for provided timeout.
	 * @param timeout the timeout in milliseconds.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B barrier(long timeout) {
		return barrier(timeout, null);
	}

	/**
	 * Populate a {@link org.springframework.integration.aggregator.BarrierMessageHandler}
	 * instance for provided timeout and options from {@link BarrierSpec} and endpoint
	 * options from {@link GenericEndpointSpec}.
	 * @param timeout the timeout in milliseconds.
	 * @param barrierConfigurer the {@link Consumer} to provide
	 * {@link org.springframework.integration.aggregator.BarrierMessageHandler} options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B barrier(long timeout, Consumer<BarrierSpec> barrierConfigurer) {
		return register(new BarrierSpec(timeout), barrierConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} instance to perform {@link MessageTriggerAction}.
	 * @param triggerActionId the {@link MessageTriggerAction} bean id.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B trigger(String triggerActionId) {
		return trigger(triggerActionId, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} instance to perform {@link MessageTriggerAction}
	 * and endpoint options from {@link GenericEndpointSpec}.
	 * @param triggerActionId the {@link MessageTriggerAction} bean id.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B trigger(String triggerActionId,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {

		MessageProcessor<Void> trigger = new BeanNameMessageProcessor<>(triggerActionId, "trigger");
		return handle(new ServiceActivatingHandler(trigger), endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} instance to perform {@link MessageTriggerAction}.
	 * @param triggerAction the {@link MessageTriggerAction}.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B trigger(MessageTriggerAction triggerAction) {
		return trigger(triggerAction, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} instance to perform {@link MessageTriggerAction}
	 * and endpoint options from {@link GenericEndpointSpec}.
	 * @param triggerAction the {@link MessageTriggerAction}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	public B trigger(MessageTriggerAction triggerAction,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {

		return handle(new ServiceActivatingHandler(triggerAction, "trigger"), endpointConfigurer);
	}

	/**
	 * Add one or more {@link ChannelInterceptor} implementations
	 * to the current {@link #currentMessageChannel}, in the given order, after any interceptors already registered.
	 * @param interceptorArray one or more {@link ChannelInterceptor}s.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 * @throws IllegalArgumentException if one or more null arguments are provided
	 * @since 5.3
	 */
	public B intercept(ChannelInterceptor... interceptorArray) {
		Assert.notNull(interceptorArray, "'interceptorArray' must not be null");
		Assert.noNullElements(interceptorArray, "'interceptorArray' must not contain null elements");

		InterceptableChannel currentChannel = currentInterceptableChannel();
		for (ChannelInterceptor interceptor : interceptorArray) {
			currentChannel.addInterceptor(interceptor);
		}

		return _this();
	}

	/**
	 * Populate a {@link FluxMessageChannel} to start a reactive processing for upstream data,
	 * wrap it to a {@link Flux}, apply provided {@link Function} via {@link Flux#transform(Function)}
	 * and emit the result to one more {@link FluxMessageChannel}, subscribed in the downstream flow.
	 * @param fluxFunction the {@link Function} to process data reactive manner.
	 * @param <I> the input payload type.
	 * @param <O> the output type.
	 * @return the current {@link BaseIntegrationFlowDefinition}.
	 */
	@SuppressWarnings(UNCHECKED)
	public <I, O> B fluxTransform(Function<? super Flux<Message<I>>, ? extends Publisher<O>> fluxFunction) {
		MessageChannel currentChannel = getCurrentMessageChannel();
		if (!(currentChannel instanceof FluxMessageChannel)) {
			currentChannel = new FluxMessageChannel();
			channel(currentChannel);
		}

		Publisher<Message<I>> upstream = (Publisher<Message<I>>) currentChannel;

		Flux<Message<O>> result = Transformers.transformWithFunction(upstream, fluxFunction);

		FluxMessageChannel downstream = new FluxMessageChannel();
		downstream.subscribeTo((Flux<Message<?>>) (Flux<?>) result);

		return currentMessageChannel(downstream)
				.addComponent(downstream);
	}

	/**
	 * Add a {@value IntegrationContextUtils#NULL_CHANNEL_BEAN_NAME} bean into this flow
	 * definition as a terminal operator.
	 * @return The {@link IntegrationFlow} instance based on this definition.
	 * @since 5.1
	 */
	public IntegrationFlow nullChannel() {
		return channel(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)
				.get();
	}

	/**
	 * Represent an Integration Flow as a Reactive Streams {@link Publisher} bean.
	 * @param <T> the expected {@code payload} type
	 * @return the Reactive Streams {@link Publisher}
	 */
	@SuppressWarnings(UNCHECKED)
	protected <T> Publisher<Message<T>> toReactivePublisher() {
		MessageChannel channelForPublisher = getCurrentMessageChannel();
		Publisher<Message<T>> publisher;
		Map<Object, String> components = getIntegrationComponents();
		if (channelForPublisher instanceof Publisher) {
			publisher = (Publisher<Message<T>>) channelForPublisher;
		}
		else {
			if (channelForPublisher != null && components.size() > 1
					&& !(channelForPublisher instanceof MessageChannelReference) &&
					!(channelForPublisher instanceof FixedSubscriberChannelPrototype)) {
				publisher = IntegrationReactiveUtils.messageChannelToFlux(channelForPublisher);
			}
			else {
				MessageChannel reactiveChannel = new FluxMessageChannel();
				publisher = (Publisher<Message<T>>) reactiveChannel;
				channel(reactiveChannel);
			}
		}

		setImplicitChannel(false);

		get();

		return new PublisherIntegrationFlow<>(components, publisher);
	}

	protected <S extends ConsumerEndpointSpec<? super S, ? extends MessageHandler>> B register(S endpointSpec,
			Consumer<S> endpointConfigurer) {

		if (endpointConfigurer != null) {
			endpointConfigurer.accept(endpointSpec);
		}

		MessageChannel inputChannel = getCurrentMessageChannel();
		currentMessageChannel(null);
		if (inputChannel == null) {
			inputChannel = new DirectChannel();
			this.registerOutputChannelIfCan(inputChannel);
		}

		Tuple2<ConsumerEndpointFactoryBean, ? extends MessageHandler> factoryBeanTuple2 = endpointSpec.get();

		addComponents(endpointSpec.getComponentsToRegister());

		if (inputChannel instanceof MessageChannelReference) {
			factoryBeanTuple2.getT1().setInputChannelName(((MessageChannelReference) inputChannel).getName());
		}
		else {
			if (inputChannel instanceof FixedSubscriberChannelPrototype) {
				String beanName = ((FixedSubscriberChannelPrototype) inputChannel).getName();
				inputChannel = new FixedSubscriberChannel(factoryBeanTuple2.getT2());
				if (beanName != null) {
					((FixedSubscriberChannel) inputChannel).setBeanName(beanName);
				}
				registerOutputChannelIfCan(inputChannel);
			}
			factoryBeanTuple2.getT1().setInputChannel(inputChannel);
		}

		return addComponent(endpointSpec).currentComponent(factoryBeanTuple2.getT2());
	}

	protected B registerOutputChannelIfCan(MessageChannel outputChannel) {
		if (!(outputChannel instanceof FixedSubscriberChannelPrototype)) {
			addComponent(outputChannel, null);
			Object currComponent = getCurrentComponent();
			if (currComponent != null) {
				String channelName = null;
				if (outputChannel instanceof MessageChannelReference) {
					channelName = ((MessageChannelReference) outputChannel).getName();
				}

				if (currComponent instanceof MessageProducer) {
					MessageProducer messageProducer = (MessageProducer) currComponent;
					checkReuse(messageProducer);
					if (channelName != null) {
						messageProducer.setOutputChannelName(channelName);
					}
					else {
						messageProducer.setOutputChannel(outputChannel);
					}
				}
				else if (currComponent instanceof SourcePollingChannelAdapterSpec) {
					SourcePollingChannelAdapterFactoryBean pollingChannelAdapterFactoryBean =
							((SourcePollingChannelAdapterSpec) currComponent).get().getT1();
					if (channelName != null) {
						pollingChannelAdapterFactoryBean.setOutputChannelName(channelName);
					}
					else {
						pollingChannelAdapterFactoryBean.setOutputChannel(outputChannel);
					}
				}
				else {
					throw new BeanCreationException("The 'currentComponent' (" + currComponent +
							") is a one-way 'MessageHandler' and it isn't appropriate to configure 'outputChannel'. " +
							"This is the end of the integration flow.");
				}
				currentComponent(null);
			}
		}
		return _this();
	}

	protected boolean isOutputChannelRequired() {
		Object currentElement = getCurrentComponent();
		if (currentElement != null) {
			if (AopUtils.isAopProxy(currentElement)) {
				currentElement = extractProxyTarget(currentElement);
			}

			return currentElement instanceof AbstractMessageProducingHandler
					|| currentElement instanceof SourcePollingChannelAdapterSpec;
		}
		return false;
	}

	@SuppressWarnings(UNCHECKED)
	protected final B _this() { // NOSONAR name
		return (B) this;
	}

	protected StandardIntegrationFlow get() {
		if (this.integrationFlow == null) {
			MessageChannel currentChannel = getCurrentMessageChannel();
			if (currentChannel instanceof FixedSubscriberChannelPrototype) {
				throw new BeanCreationException("The 'currentMessageChannel' (" + currentChannel +
						") is a prototype for 'FixedSubscriberChannel' which can't be created without " +
						"a 'MessageHandler' constructor argument. " +
						"That means that '.fixedSubscriberChannel()' can't be the last " +
						"EIP-method in the 'IntegrationFlow' definition.");
			}

			Map<Object, String> components = getIntegrationComponents();
			if (components.size() == 1) {
				Object currComponent = getCurrentComponent();
				if (currComponent != null) {
					if (currComponent instanceof SourcePollingChannelAdapterSpec) {
						throw new BeanCreationException("The 'SourcePollingChannelAdapter' (" + currComponent
								+ ") " + "must be configured with at least one 'MessageChannel' or 'MessageHandler'.");
					}
				}
				else if (currentChannel != null) {
					throw new BeanCreationException("The 'IntegrationFlow' can't consist of only one 'MessageChannel'. "
							+ "Add at least '.bridge()' EIP-method before the end of flow.");
				}
			}

			if (isImplicitChannel()) {
				Optional<Object> lastComponent =
						components.keySet()
								.stream()
								.reduce((first, second) -> second);
				if (lastComponent.get() instanceof WireTapSpec) {
					channel(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME);
				}
			}

			this.integrationFlow = new StandardIntegrationFlow(components);
		}
		return this.integrationFlow;
	}

	protected void checkReuse(MessageProducer replyHandler) {
		Assert.isTrue(!REFERENCED_REPLY_PRODUCERS.contains(replyHandler),
				"A reply MessageProducer may only be referenced once ("
						+ replyHandler
						+ ") - use @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) on @Bean definition.");
		REFERENCED_REPLY_PRODUCERS.add(replyHandler);
	}

	protected static Object extractProxyTarget(Object target) {
		if (!(target instanceof Advised)) {
			return target;
		}
		Advised advised = (Advised) target;
		try {
			return extractProxyTarget(advised.getTargetSource().getTarget());
		}
		catch (Exception e) {
			throw new BeanCreationException("Could not extract target", e);
		}
	}

	public static final class ReplyProducerCleaner implements DestructionAwareBeanPostProcessor {

		private ReplyProducerCleaner() {
		}

		@Override
		public boolean requiresDestruction(Object bean) {
			return BaseIntegrationFlowDefinition.REFERENCED_REPLY_PRODUCERS.contains(bean);
		}

		@Override
		public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
			BaseIntegrationFlowDefinition.REFERENCED_REPLY_PRODUCERS.remove(bean);
		}

	}

}
