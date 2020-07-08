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

import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.integration.transformer.MessageTransformingHandler;

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

}
