/*
 * Copyright 2016-2020 the original author or authors.
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
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.expression.Expression;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.MessageTriggerAction;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.ErrorMessageExceptionTypeRouter;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.splitter.ExpressionEvaluatingSplitter;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MapBuilder;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.integration.transformer.HeaderFilter;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import reactor.core.publisher.Flux;

/**
 * The {@code BaseIntegrationFlowDefinition} extension for syntax sugar with generics for some
 * type-based EIP-methods when an expected payload type is assumed from upstream.
 * For more explicit type conversion the methods with a {@link Class} argument are recommended for use.
 *
 * @param <B> the {@link IntegrationFlowDefinition} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Gabriele Del Prete
 *
 * @since 5.0
 */
public abstract class IntegrationFlowDefinition<B extends IntegrationFlowDefinition<B>>
		extends BaseIntegrationFlowDefinition<B> {

	IntegrationFlowDefinition() {
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the provided
	 * {@link GenericTransformer}. Use {@link #transform(Class, GenericTransformer)} if
	 * you need to access the entire message.
	 * @param genericTransformer the {@link GenericTransformer} to populate.
	 * @param <S> the source type - 'transform from'.
	 * @param <T> the target type - 'transform to'.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see org.springframework.integration.transformer.MethodInvokingTransformer
	 * @see org.springframework.integration.handler.LambdaMessageProcessor
	 */
	public <S, T> B transform(GenericTransformer<S, T> genericTransformer) {
		return transform(null, genericTransformer);
	}


	/**
	 * Populate the {@link MessageTransformingHandler} instance for the provided
	 * {@link GenericTransformer}. In addition accept options for the integration endpoint
	 * using {@link GenericEndpointSpec}. Use
	 * {@link #transform(Class, GenericTransformer, Consumer)} if you need to access the
	 * entire message.
	 * @param genericTransformer the {@link GenericTransformer} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint
	 * options.
	 * @param <S> the source type - 'transform from'.
	 * @param <T> the target type - 'transform to'.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see org.springframework.integration.transformer.MethodInvokingTransformer
	 * @see org.springframework.integration.handler.LambdaMessageProcessor
	 * @see GenericEndpointSpec
	 */
	public <S, T> B transform(GenericTransformer<S, T> genericTransformer,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {

		return transform(null, genericTransformer, endpointConfigurer);
	}

	/**
	 * Populate a {@link org.springframework.integration.filter.MessageFilter}
	 * with {@link org.springframework.integration.filter.MethodInvokingSelector}
	 * for the provided {@link GenericSelector}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .filter("World"::equals)
	 * }
	 * </pre>
	 * Use {@link #filter(Class, GenericSelector)} if you need to access the entire
	 * message.
	 * @param genericSelector the {@link GenericSelector} to use.
	 * @param <P> the source payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <P> B filter(GenericSelector<P> genericSelector) {
		return filter(null, genericSelector);
	}

	/**
	 * Populate a {@link org.springframework.integration.filter.MessageFilter}
	 * with {@link org.springframework.integration.filter.MethodInvokingSelector}
	 * for the provided {@link GenericSelector}.
	 * In addition accept options for the integration endpoint using {@link FilterEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .filter("World"::equals, e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * Use {@link #filter(Class, GenericSelector, Consumer)} if you need to access the entire
	 * message.
	 * @param genericSelector the {@link GenericSelector} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the source payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see FilterEndpointSpec
	 */
	public <P> B filter(GenericSelector<P> genericSelector, Consumer<FilterEndpointSpec> endpointConfigurer) {
		return filter(null, genericSelector, endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the provided {@link GenericHandler} at runtime.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .<Integer>handle((p, h) -> p / 2)
	 * }
	 * </pre>
	 * Use {@link #handle(Class, GenericHandler)} if you need to access the entire
	 * message.
	 * @param handler the handler to invoke.
	 * @param <P> the payload type to expect.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see org.springframework.integration.handler.LambdaMessageProcessor
	 */
	public <P> B handle(GenericHandler<P> handler) {
		return handle(null, handler);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the provided {@link GenericHandler} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .<Integer>handle((p, h) -> p / 2, e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * Use {@link #handle(Class, GenericHandler, Consumer)} if you need to access the entire
	 * message.
	 * @param handler the handler to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type to expect.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see org.springframework.integration.handler.LambdaMessageProcessor
	 * @see GenericEndpointSpec
	 */
	public <P> B handle(GenericHandler<P> handler,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {

		return handle(null, handler, endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@link Function} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .<String>split(p ->
	 *        jdbcTemplate.execute("SELECT * from FOO",
	 *            (PreparedStatement ps) ->
	 *                 new ResultSetIterator<Foo>(ps.executeQuery(),
	 *                     (rs, rowNum) ->
	 *                           new Foo(rs.getInt(1), rs.getString(2))))
	 *       , e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param splitter the splitter {@link Function}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see org.springframework.integration.handler.LambdaMessageProcessor
	 * @see SplitterEndpointSpec
	 */
	public <P> B split(Function<P, ?> splitter,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) {

		return split(null, splitter, endpointConfigurer);
	}


	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * with default options.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .route(p -> p.equals("foo") || p.equals("bar") ? new String[] {"foo", "bar"} : null)
	 * }
	 * </pre>
	 * Use {@link #route(Class, Function)} if you need to access the entire message.
	 * @param router the {@link Function} to use.
	 * @param <S> the source payload type.
	 * @param <T> the target result type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <S, T> B route(Function<S, T> router) {
		return route(null, router);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * with provided options from {@link RouterSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .<Integer, Boolean>route(p -> p % 2 == 0,
	 *                 m -> m.channelMapping("true", "evenChannel")
	 *                       .subFlowMapping("false", f ->
	 *                                   f.<Integer>handle((p, h) -> p * 3))
	 *                       .applySequence(false))
	 * }
	 * </pre>
	 * Use {@link #route(Class, Function, Consumer)} if you need to access the entire message.
	 * @param router the {@link Function} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param <S> the source payload type.
	 * @param <T> the target result type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <S, T> B route(Function<S, T> router, Consumer<RouterSpec<T, MethodInvokingRouter>> routerConfigurer) {
		return route(null, router, routerConfigurer);
	}


	// All the methods below override super for byte code backward compatibility.

	@Override
	public B fixedSubscriberChannel() { // NOSONAR - byte code backward compatibility
		return super.fixedSubscriberChannel();
	}

	@Override
	public B fixedSubscriberChannel(String messageChannelName) { // NOSONAR - byte code backward compatibility
		return super.fixedSubscriberChannel(messageChannelName);
	}

	@Override
	public B channel(String messageChannelName) { // NOSONAR - byte code backward compatibility
		return super.channel(messageChannelName);
	}

	@Override
	public B channel(MessageChannelSpec<?, ?> messageChannelSpec) { // NOSONAR - byte code backward compatibility
		return super.channel(messageChannelSpec);
	}

	@Override
	public B channel(MessageChannel messageChannel) { // NOSONAR - byte code backward compatibility
		return super.channel(messageChannel);
	}

	@Override
	public B channel(Function<Channels, MessageChannelSpec<?, ?>> channels) { // NOSONAR - byte code backward compatibility
		return super.channel(channels);
	}

	@Override
	public B publishSubscribeChannel(Consumer<PublishSubscribeSpec> publishSubscribeChannelConfigurer) { // NOSONAR - byte code backward compatibility
		return super.publishSubscribeChannel(publishSubscribeChannelConfigurer);
	}

	@Override
	public B publishSubscribeChannel(Executor executor,
			Consumer<PublishSubscribeSpec> publishSubscribeChannelConfigurer) { // NOSONAR - byte code backward compatibility

		return super.publishSubscribeChannel(executor, publishSubscribeChannelConfigurer);
	}

	@Override
	public B wireTap(IntegrationFlow flow) { // NOSONAR - byte code backward compatibility
		return super.wireTap(flow);
	}

	@Override
	public B wireTap(String wireTapChannel) { // NOSONAR - byte code backward compatibility
		return super.wireTap(wireTapChannel);
	}

	@Override
	public B wireTap(MessageChannel wireTapChannel) { // NOSONAR - byte code backward compatibility
		return super.wireTap(wireTapChannel);
	}

	@Override
	public B wireTap(IntegrationFlow flow, Consumer<WireTapSpec> wireTapConfigurer) { // NOSONAR - byte code backward compatibility
		return super.wireTap(flow, wireTapConfigurer);
	}

	@Override
	public B wireTap(String wireTapChannel, Consumer<WireTapSpec> wireTapConfigurer) { // NOSONAR - byte code backward compatibility
		return super.wireTap(wireTapChannel, wireTapConfigurer);
	}

	@Override
	public B wireTap(MessageChannel wireTapChannel, Consumer<WireTapSpec> wireTapConfigurer) { // NOSONAR - byte code backward compatibility
		return super.wireTap(wireTapChannel, wireTapConfigurer);
	}

	@Override
	public B wireTap(WireTapSpec wireTapSpec) { // NOSONAR - byte code backward compatibility
		return super.wireTap(wireTapSpec);
	}

	@Override
	public B controlBus() { // NOSONAR - byte code backward compatibility
		return super.controlBus();
	}

	@Override
	public B controlBus(Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.controlBus(endpointConfigurer);
	}

	@Override
	public B transform(String expression) { // NOSONAR - byte code backward compatibility
		return super.transform(expression);
	}

	@Override
	public B transform(String expression,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.transform(expression, endpointConfigurer);
	}

	@Override
	public B transform(Object service) { // NOSONAR - byte code backward compatibility
		return super.transform(service);
	}

	@Override
	public B transform(Object service, String methodName) { // NOSONAR - byte code backward compatibility
		return super.transform(service, methodName);
	}

	@Override
	public B transform(Object service, String methodName,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.transform(service, methodName, endpointConfigurer);
	}

	@Override
	public B transform(MessageProcessorSpec<?> messageProcessorSpec) { // NOSONAR - byte code backward compatibility
		return super.transform(messageProcessorSpec);
	}

	@Override
	public B transform(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.transform(messageProcessorSpec, endpointConfigurer);
	}

	@Override
	public <P> B convert(Class<P> payloadType) { // NOSONAR - byte code backward compatibility
		return super.convert(payloadType);
	}

	@Override
	public <P, T> B transform(Class<P> payloadType, GenericTransformer<P, T> genericTransformer) { // NOSONAR - byte code backward compatibility
		return super.transform(payloadType, genericTransformer);
	}

	@Override
	public <P> B convert(Class<P> payloadType,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.convert(payloadType, endpointConfigurer);
	}

	@Override
	public <P, T> B transform(Class<P> payloadType, GenericTransformer<P, T> genericTransformer,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.transform(payloadType, genericTransformer, endpointConfigurer);
	}

	@Override
	public B filter(String expression) { // NOSONAR - byte code backward compatibility
		return super.filter(expression);
	}

	@Override
	public B filter(String expression, Consumer<FilterEndpointSpec> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.filter(expression, endpointConfigurer);
	}

	@Override
	public B filter(Object service) { // NOSONAR - byte code backward compatibility
		return super.filter(service);
	}

	@Override
	public B filter(Object service, String methodName) { // NOSONAR - byte code backward compatibility
		return super.filter(service, methodName);
	}

	@Override
	public B filter(Object service, String methodName, Consumer<FilterEndpointSpec> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.filter(service, methodName, endpointConfigurer);
	}

	@Override
	public B filter(MessageProcessorSpec<?> messageProcessorSpec) { // NOSONAR - byte code backward compatibility
		return super.filter(messageProcessorSpec);
	}

	@Override
	public B filter(MessageProcessorSpec<?> messageProcessorSpec, Consumer<FilterEndpointSpec> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.filter(messageProcessorSpec, endpointConfigurer);
	}

	@Override
	public <P> B filter(Class<P> payloadType, GenericSelector<P> genericSelector) { // NOSONAR - byte code backward compatibility
		return super.filter(payloadType, genericSelector);
	}

	@Override
	public <P> B filter(Class<P> payloadType, GenericSelector<P> genericSelector,
			Consumer<FilterEndpointSpec> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.filter(payloadType, genericSelector, endpointConfigurer);
	}

	@Override
	public <H extends MessageHandler> B handle(MessageHandlerSpec<?, H> messageHandlerSpec) { // NOSONAR - byte code backward compatibility
		return super.handle(messageHandlerSpec);
	}

	@Override
	public B handle(MessageHandler messageHandler) { // NOSONAR - byte code backward compatibility
		return super.handle(messageHandler);
	}

	@Override
	public B handle(String beanName, String methodName) { // NOSONAR - byte code backward compatibility
		return super.handle(beanName, methodName);
	}

	@Override
	public B handle(String beanName, String methodName,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.handle(beanName, methodName, endpointConfigurer);
	}

	@Override
	public B handle(Object service) { // NOSONAR - byte code backward compatibility
		return super.handle(service);
	}

	@Override
	public B handle(Object service, String methodName) { // NOSONAR - byte code backward compatibility
		return super.handle(service, methodName);
	}

	@Override
	public B handle(Object service, String methodName,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.handle(service, methodName, endpointConfigurer);
	}

	@Override
	public <P> B handle(Class<P> payloadType, GenericHandler<P> handler) { // NOSONAR - byte code backward compatibility
		return super.handle(payloadType, handler);
	}

	@Override
	public <P> B handle(Class<P> payloadType, GenericHandler<P> handler,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.handle(payloadType, handler, endpointConfigurer);
	}

	@Override
	public B handle(MessageProcessorSpec<?> messageProcessorSpec) { // NOSONAR - byte code backward compatibility
		return super.handle(messageProcessorSpec);
	}

	@Override
	public B handle(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.handle(messageProcessorSpec, endpointConfigurer);
	}

	@Override
	public <H extends MessageHandler> B handle(MessageHandlerSpec<?, H> messageHandlerSpec,
			Consumer<GenericEndpointSpec<H>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.handle(messageHandlerSpec, endpointConfigurer);
	}

	@Override
	public <H extends MessageHandler> B handle(H messageHandler, Consumer<GenericEndpointSpec<H>> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.handle(messageHandler, endpointConfigurer);
	}

	@Override
	public B bridge() { // NOSONAR - byte code backward compatibility
		return super.bridge();
	}

	@Override
	public B bridge(Consumer<GenericEndpointSpec<BridgeHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.bridge(endpointConfigurer);
	}

	@Override
	public B delay(String groupId) { // NOSONAR - byte code backward compatibility
		return super.delay(groupId);
	}

	@Override
	public B delay(String groupId, Consumer<DelayerEndpointSpec> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.delay(groupId, endpointConfigurer);
	}

	@Override
	public B enrich(Consumer<EnricherSpec> enricherConfigurer) { // NOSONAR - byte code backward compatibility
		return super.enrich(enricherConfigurer);
	}

	@Override
	public B enrichHeaders(MapBuilder<?, String, Object> headers) { // NOSONAR - byte code backward compatibility
		return super.enrichHeaders(headers);
	}

	@Override
	public B enrichHeaders(MapBuilder<?, String, Object> headers,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.enrichHeaders(headers, endpointConfigurer);
	}

	@Override
	public B enrichHeaders(Map<String, Object> headers,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.enrichHeaders(headers, endpointConfigurer);
	}

	@Override
	public B enrichHeaders(Consumer<HeaderEnricherSpec> headerEnricherConfigurer) { // NOSONAR - byte code backward compatibility
		return super.enrichHeaders(headerEnricherConfigurer);
	}

	@Override
	public B split() { // NOSONAR - byte code backward compatibility
		return super.split();
	}

	@Override
	public B split(Consumer<SplitterEndpointSpec<DefaultMessageSplitter>> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.split(endpointConfigurer);
	}

	@Override
	public B split(String expression) { // NOSONAR - byte code backward compatibility
		return super.split(expression);
	}

	@Override
	public B split(String expression, Consumer<SplitterEndpointSpec<ExpressionEvaluatingSplitter>> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.split(expression, endpointConfigurer);
	}

	@Override
	public B split(Object service) { // NOSONAR - byte code backward compatibility
		return super.split(service);
	}

	@Override
	public B split(Object service, String methodName) { // NOSONAR - byte code backward compatibility
		return super.split(service, methodName);
	}

	@Override
	public B split(Object service, String methodName,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.split(service, methodName, endpointConfigurer);
	}

	@Override
	public B split(String beanName, String methodName) { // NOSONAR - byte code backward compatibility
		return super.split(beanName, methodName);
	}

	@Override
	public B split(String beanName, String methodName,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.split(beanName, methodName, endpointConfigurer);
	}

	@Override
	public B split(MessageProcessorSpec<?> messageProcessorSpec) { // NOSONAR - byte code backward compatibility
		return super.split(messageProcessorSpec);
	}

	@Override
	public B split(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.split(messageProcessorSpec, endpointConfigurer);
	}

	@Override
	public <P> B split(Class<P> payloadType, Function<P, ?> splitter) { // NOSONAR - byte code backward compatibility
		return super.split(payloadType, splitter);
	}

	@Override
	public <P> B split(Class<P> payloadType, Function<P, ?> splitter,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.split(payloadType, splitter, endpointConfigurer);
	}

	@Override
	public <S extends AbstractMessageSplitter> B split(MessageHandlerSpec<?, S> splitterMessageHandlerSpec) { // NOSONAR - byte code backward compatibility
		return super.split(splitterMessageHandlerSpec);
	}

	@Override
	public <S extends AbstractMessageSplitter> B split(MessageHandlerSpec<?, S> splitterMessageHandlerSpec,
			Consumer<SplitterEndpointSpec<S>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.split(splitterMessageHandlerSpec, endpointConfigurer);
	}

	@Override
	public B split(AbstractMessageSplitter splitter) { // NOSONAR - byte code backward compatibility
		return super.split(splitter);
	}

	@Override
	public <S extends AbstractMessageSplitter> B split(S splitter,
			Consumer<SplitterEndpointSpec<S>> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.split(splitter, endpointConfigurer);
	}

	@Override
	public B headerFilter(String... headersToRemove) { // NOSONAR - byte code backward compatibility
		return super.headerFilter(headersToRemove);
	}

	@Override
	public B headerFilter(String headersToRemove, boolean patternMatch) { // NOSONAR - byte code backward compatibility
		return super.headerFilter(headersToRemove, patternMatch);
	}

	@Override
	public B headerFilter(HeaderFilter headerFilter,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.headerFilter(headerFilter, endpointConfigurer);
	}

	@Override
	public B claimCheckIn(MessageStore messageStore) { // NOSONAR - byte code backward compatibility
		return super.claimCheckIn(messageStore);
	}

	@Override
	public B claimCheckIn(MessageStore messageStore,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.claimCheckIn(messageStore, endpointConfigurer);
	}

	@Override
	public B claimCheckOut(MessageStore messageStore) { // NOSONAR - byte code backward compatibility
		return super.claimCheckOut(messageStore);
	}

	@Override
	public B claimCheckOut(MessageStore messageStore, boolean removeMessage) { // NOSONAR - byte code backward compatibility
		return super.claimCheckOut(messageStore, removeMessage);
	}

	@Override
	public B claimCheckOut(MessageStore messageStore, boolean removeMessage,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.claimCheckOut(messageStore, removeMessage, endpointConfigurer);
	}

	@Override
	public B resequence() { // NOSONAR - byte code backward compatibility
		return super.resequence();
	}

	@Override
	public B resequence(Consumer<ResequencerSpec> resequencer) { // NOSONAR - byte code backward compatibility
		return super.resequence(resequencer);
	}

	@Override
	public B aggregate() { // NOSONAR - byte code backward compatibility
		return super.aggregate();
	}

	@Override
	public B aggregate(Consumer<AggregatorSpec> aggregator) { // NOSONAR - byte code backward compatibility
		return super.aggregate(aggregator);
	}

	@Override
	public B route(String beanName, String method) { // NOSONAR - byte code backward compatibility
		return super.route(beanName, method);
	}

	@Override
	public B route(String beanName, String method,
			Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer) { // NOSONAR - byte code backward compatibility

		return super.route(beanName, method, routerConfigurer);
	}

	@Override
	public B route(Object service) { // NOSONAR - byte code backward compatibility
		return super.route(service);
	}

	@Override
	public B route(Object service, String methodName) { // NOSONAR - byte code backward compatibility
		return super.route(service, methodName);
	}

	@Override
	public B route(Object service, String methodName,
			Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer) { // NOSONAR - byte code backward compatibility

		return super.route(service, methodName, routerConfigurer);
	}

	@Override
	public B route(String expression) { // NOSONAR - byte code backward compatibility
		return super.route(expression);
	}

	@Override
	public <T> B route(String expression, Consumer<RouterSpec<T, ExpressionEvaluatingRouter>> routerConfigurer) { // NOSONAR - byte code backward compatibility
		return super.route(expression, routerConfigurer);
	}

	@Override
	public <S, T> B route(Class<S> payloadType, Function<S, T> router) { // NOSONAR - byte code backward compatibility
		return super.route(payloadType, router);
	}

	@Override
	public <P, T> B route(Class<P> payloadType, Function<P, T> router,
			Consumer<RouterSpec<T, MethodInvokingRouter>> routerConfigurer) { // NOSONAR - byte code backward compatibility
		return super.route(payloadType, router, routerConfigurer);
	}

	@Override
	public B route(MessageProcessorSpec<?> messageProcessorSpec) { // NOSONAR - byte code backward compatibility
		return super.route(messageProcessorSpec);
	}

	@Override
	public B route(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer) { // NOSONAR - byte code backward compatibility

		return super.route(messageProcessorSpec, routerConfigurer);
	}

	@Override
	public B routeToRecipients(Consumer<RecipientListRouterSpec> routerConfigurer) { // NOSONAR - byte code backward compatibility
		return super.routeToRecipients(routerConfigurer);
	}

	@Override
	public B routeByException(Consumer<RouterSpec<Class<? extends Throwable>,  // NOSONAR - byte code backward compatibility
			ErrorMessageExceptionTypeRouter>> routerConfigurer) {

		return super.routeByException(routerConfigurer);
	}

	@Override
	public B route(AbstractMessageRouter router) { // NOSONAR - byte code backward compatibility
		return super.route(router);
	}

	@Override
	public <R extends AbstractMessageRouter> B route(R router, Consumer<GenericEndpointSpec<R>> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.route(router, endpointConfigurer);
	}

	@Override
	public B gateway(String requestChannel) { // NOSONAR - byte code backward compatibility
		return super.gateway(requestChannel);
	}

	@Override
	public B gateway(String requestChannel, Consumer<GatewayEndpointSpec> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.gateway(requestChannel, endpointConfigurer);
	}

	@Override
	public B gateway(MessageChannel requestChannel) { // NOSONAR - byte code backward compatibility
		return super.gateway(requestChannel);
	}

	@Override
	public B gateway(MessageChannel requestChannel, Consumer<GatewayEndpointSpec> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.gateway(requestChannel, endpointConfigurer);
	}

	@Override
	public B gateway(IntegrationFlow flow) { // NOSONAR - byte code backward compatibility
		return super.gateway(flow);
	}

	@Override
	public B gateway(IntegrationFlow flow, Consumer<GatewayEndpointSpec> endpointConfigurer) { // NOSONAR - byte code backward compatibility
		return super.gateway(flow, endpointConfigurer);
	}

	@Override
	public B log() { // NOSONAR - byte code backward compatibility
		return super.log();
	}

	@Override
	public B log(LoggingHandler.Level level) { // NOSONAR - byte code backward compatibility
		return super.log(level);
	}

	@Override
	public B log(String category) { // NOSONAR - byte code backward compatibility
		return super.log(category);
	}

	@Override
	public B log(LoggingHandler.Level level, String category) { // NOSONAR - byte code backward compatibility
		return super.log(level, category);
	}

	@Override
	public B log(LoggingHandler.Level level, String category, String logExpression) { // NOSONAR - byte code backward compatibility
		return super.log(level, category, logExpression);
	}

	@Override
	public <P> B log(Function<Message<P>, Object> function) { // NOSONAR - byte code backward compatibility
		return super.log(function);
	}

	@Override
	public B log(Expression logExpression) { // NOSONAR - byte code backward compatibility
		return super.log(logExpression);
	}

	@Override
	public B log(LoggingHandler.Level level, Expression logExpression) { // NOSONAR - byte code backward compatibility
		return super.log(level, logExpression);
	}

	@Override
	public B log(String category, Expression logExpression) { // NOSONAR - byte code backward compatibility
		return super.log(category, logExpression);
	}

	@Override
	public <P> B log(LoggingHandler.Level level, Function<Message<P>, Object> function) { // NOSONAR - byte code backward compatibility
		return super.log(level, function);
	}

	@Override
	public <P> B log(String category, Function<Message<P>, Object> function) { // NOSONAR - byte code backward compatibility
		return super.log(category, function);
	}

	@Override
	public <P> B log(LoggingHandler.Level level, String category, Function<Message<P>, Object> function) { // NOSONAR - byte code backward compatibility
		return super.log(level, category, function);
	}

	@Override
	public B log(LoggingHandler.Level level, String category, Expression logExpression) { // NOSONAR - byte code backward compatibility
		return super.log(level, category, logExpression);
	}

	@Override
	public IntegrationFlow logAndReply() { // NOSONAR - byte code backward compatibility
		return super.logAndReply();
	}

	@Override
	public IntegrationFlow logAndReply(LoggingHandler.Level level) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(level);
	}

	@Override
	public IntegrationFlow logAndReply(String category) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(category);
	}

	@Override
	public IntegrationFlow logAndReply(LoggingHandler.Level level, String category) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(level, category);
	}

	@Override
	public IntegrationFlow logAndReply(LoggingHandler.Level level, String category, String logExpression) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(level, category, logExpression);
	}

	@Override
	public <P> IntegrationFlow logAndReply(Function<Message<P>, Object> function) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(function);
	}

	@Override
	public IntegrationFlow logAndReply(Expression logExpression) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(logExpression);
	}

	@Override
	public IntegrationFlow logAndReply(LoggingHandler.Level level, Expression logExpression) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(level, logExpression);
	}

	@Override
	public IntegrationFlow logAndReply(String category, Expression logExpression) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(category, logExpression);
	}

	@Override
	public <P> IntegrationFlow logAndReply(LoggingHandler.Level level, Function<Message<P>, Object> function) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(level, function);
	}

	@Override
	public <P> IntegrationFlow logAndReply(String category, Function<Message<P>, Object> function) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(category, function);
	}

	@Override
	public <P> IntegrationFlow logAndReply(LoggingHandler.Level level, String category,
			Function<Message<P>, Object> function) { // NOSONAR - byte code backward compatibility

		return super.logAndReply(level, category, function);
	}

	@Override
	public IntegrationFlow logAndReply(LoggingHandler.Level level, String category, Expression logExpression) { // NOSONAR - byte code backward compatibility
		return super.logAndReply(level, category, logExpression);
	}

	@Override
	public B scatterGather(MessageChannel scatterChannel) { // NOSONAR - byte code backward compatibility
		return super.scatterGather(scatterChannel);
	}

	@Override
	public B scatterGather(MessageChannel scatterChannel, Consumer<AggregatorSpec> gatherer) { // NOSONAR - byte code backward compatibility
		return super.scatterGather(scatterChannel, gatherer);
	}

	@Override
	public B scatterGather(MessageChannel scatterChannel, Consumer<AggregatorSpec> gatherer,
			Consumer<ScatterGatherSpec> scatterGather) { // NOSONAR - byte code backward compatibility

		return super.scatterGather(scatterChannel, gatherer, scatterGather);
	}

	@Override
	public B scatterGather(Consumer<RecipientListRouterSpec> scatterer) { // NOSONAR - byte code backward compatibility
		return super.scatterGather(scatterer);
	}

	@Override
	public B scatterGather(Consumer<RecipientListRouterSpec> scatterer, Consumer<AggregatorSpec> gatherer) { // NOSONAR - byte code backward compatibility
		return super.scatterGather(scatterer, gatherer);
	}

	@Override
	public B scatterGather(Consumer<RecipientListRouterSpec> scatterer, Consumer<AggregatorSpec> gatherer,
			Consumer<ScatterGatherSpec> scatterGather) { // NOSONAR - byte code backward compatibility

		return super.scatterGather(scatterer, gatherer, scatterGather);
	}

	@Override
	public B barrier(long timeout) { // NOSONAR - byte code backward compatibility
		return super.barrier(timeout);
	}

	@Override
	public B barrier(long timeout, Consumer<BarrierSpec> barrierConfigurer) { // NOSONAR - byte code backward compatibility
		return super.barrier(timeout, barrierConfigurer);
	}

	@Override
	public B trigger(String triggerActionId) { // NOSONAR - byte code backward compatibility
		return super.trigger(triggerActionId);
	}

	@Override
	public B trigger(String triggerActionId,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.trigger(triggerActionId, endpointConfigurer);
	}

	@Override
	public B trigger(MessageTriggerAction triggerAction) { // NOSONAR - byte code backward compatibility
		return super.trigger(triggerAction);
	}

	@Override
	public B trigger(MessageTriggerAction triggerAction,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) { // NOSONAR - byte code backward compatibility

		return super.trigger(triggerAction, endpointConfigurer);
	}

	@Override
	public <I, O> B fluxTransform(Function<? super Flux<Message<I>>, ? extends Publisher<O>> fluxFunction) { // NOSONAR - byte code backward compatibility
		return super.fluxTransform(fluxFunction);
	}

	@Override
	public IntegrationFlow nullChannel() { // NOSONAR - byte code backward compatibility
		return super.nullChannel();
	}

	@Override
	public B enrichHeaders(Map<String, Object> headers) { // NOSONAR - byte code backward compatibility
		return super.enrichHeaders(headers);
	}

}
