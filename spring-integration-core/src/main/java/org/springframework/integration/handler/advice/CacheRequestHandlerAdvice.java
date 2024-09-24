/*
 * Copyright 2019-2024 the original author or authors.
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

package org.springframework.integration.handler.advice;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * The {@link AbstractRequestHandlerAdvice} implementation for caching
 * {@code AbstractReplyProducingMessageHandler.RequestHandler#handleRequestMessage(Message)} results.
 * Supports all the cache operations - cacheable, put, evict.
 * By default, only cacheable is applied for the provided {@code cacheNames}.
 * The default cache {@code key} is {@code payload} of the request message.
 *
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 5.2
 *
 * @see CacheAspectSupport
 * @see CacheOperation
 */
public class CacheRequestHandlerAdvice extends AbstractRequestHandlerAdvice
		implements SmartInitializingSingleton {

	private static final Method HANDLE_REQUEST_METHOD;

	static {
		Class<?> requestHandlerClass = null;
		try {
			requestHandlerClass = ClassUtils.forName(
					"org.springframework.integration.handler.AbstractReplyProducingMessageHandler.RequestHandler",
					null);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
		finally {
			if (requestHandlerClass != null) {
				HANDLE_REQUEST_METHOD =
						ReflectionUtils.findMethod(requestHandlerClass, "handleRequestMessage", Message.class);
			}
			else {
				HANDLE_REQUEST_METHOD = null;
			}
		}
	}

	private final IntegrationCacheAspect delegate = new IntegrationCacheAspect();

	private final String[] cacheNames;

	private final List<CacheOperation> cacheOperations = new ArrayList<>();

	private Expression keyExpression = new FunctionExpression<Message<?>>(Message::getPayload);

	/**
	 * Create a {@link CacheRequestHandlerAdvice} instance based on the provided name of caches
	 * and {@link CacheableOperation} as default one.
	 * This can be overridden by the {@link #setCacheOperations}.
	 * @param cacheNamesArg the name of caches to use in the advice.
	 * @see #setCacheOperations
	 */
	public CacheRequestHandlerAdvice(String... cacheNamesArg) {
		this.cacheNames = cacheNamesArg != null ? Arrays.copyOf(cacheNamesArg, cacheNamesArg.length) : null;
	}

	/**
	 * Configure a set of {@link CacheOperation} which are going to be applied to the
	 * {@code AbstractReplyProducingMessageHandler.RequestHandler#handleRequestMessage(Message)}
	 * method via {@link IntegrationCacheAspect}.
	 * This is similar to the technique provided by the
	 * {@link org.springframework.cache.annotation.Caching} annotation.
	 * @param cacheOperations the array of {@link CacheOperation} to use.
	 * @see org.springframework.cache.annotation.Caching
	 */
	public void setCacheOperations(CacheOperation... cacheOperations) {
		Assert.notEmpty(cacheOperations, "'cacheOperations' must not be empty");
		Assert.notNull(cacheOperations, "'cacheOperations' must not be null");
		this.cacheOperations.clear();
		this.cacheOperations.addAll(Arrays.asList(cacheOperations));
	}

	/**
	 * Configure a common {@link CacheManager} if some {@link CacheOperation} comes without it.
	 * See {@link org.springframework.cache.annotation.CacheConfig} annotation for similar approach.
	 * @param cacheManager the {@link CacheManager} to use.
	 * @see org.springframework.cache.annotation.CacheConfig
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.delegate.setCacheManager(cacheManager);
	}

	/**
	 * Configure a common {@link CacheResolver} if some {@link CacheOperation} comes without it.
	 * See {@link org.springframework.cache.annotation.CacheConfig} for similar approach.
	 * @param cacheResolver the {@link CacheResolver} to use.
	 * @see org.springframework.cache.annotation.CacheConfig
	 */
	public void setCacheResolver(CacheResolver cacheResolver) {
		this.delegate.setCacheResolver(cacheResolver);
	}

	/**
	 * Set the {@link CacheErrorHandler} instance to use to handle errors
	 * thrown by the cache provider.
	 * @param errorHandler the {@link CacheErrorHandler} to use.
	 * @see CacheAspectSupport#setErrorHandler(CacheErrorHandler)
	 */
	public void setErrorHandler(CacheErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "'errorHandler' must not be null");
		this.delegate.setErrorHandler(errorHandler);
	}

	/**
	 * Configure an expression in SpEL style to evaluate a cache key at runtime
	 * against a request message.
	 * @param keyExpression the expression to use for cache key generation.
	 */
	public void setKeyExpressionString(String keyExpression) {
		Assert.hasText(keyExpression, "'keyExpression' must not be empty");
		setKeyExpression(EXPRESSION_PARSER.parseExpression(keyExpression));
	}

	/**
	 * Configure a {@link Function} to evaluate a cache key at runtime
	 * against a request message.
	 * @param keyFunction the {@link Function} to use for cache key generation.
	 */
	public void setKeyFunction(Function<Message<?>, ?> keyFunction) {
		Assert.notNull(keyFunction, "'keyFunction' must not be null");
		setKeyExpression(new FunctionExpression<>(keyFunction));
	}

	/**
	 * Configure a SpEL expression to evaluate a cache key at runtime
	 * against a request message.
	 * @param keyExpression the expression to use for cache key generation.
	 */
	public void setKeyExpression(Expression keyExpression) {
		Assert.notNull(keyExpression, "'keyExpression' must not be null");
		this.keyExpression = keyExpression;
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.delegate.afterSingletonsInstantiated();
	}

	@Override
	protected void onInit() {
		if (this.cacheOperations.isEmpty()) {
			CacheableOperation.Builder builder = new CacheableOperation.Builder();
			builder.setName(toString());
			this.cacheOperations.add(builder.build());
		}
		List<CacheOperation> cacheOperationsToUse;
		if (!ObjectUtils.isEmpty(this.cacheNames)) {
			cacheOperationsToUse =
					this.cacheOperations.stream()
							.filter((operation) -> ObjectUtils.isEmpty(operation.getCacheNames()))
							.map((operation) -> {
								CacheOperation.Builder builder;
								if (operation instanceof CacheableOperation cacheableOperation) {
									CacheableOperation.Builder cacheableBuilder = new CacheableOperation.Builder();
									cacheableBuilder.setSync(cacheableOperation.isSync());
									String unless = cacheableOperation.getUnless();
									if (unless != null) {
										cacheableBuilder.setUnless(unless);
									}
									builder = cacheableBuilder;
								}
								else if (operation instanceof CacheEvictOperation cacheEvictOperation) {
									CacheEvictOperation.Builder cacheEvictBuilder = new CacheEvictOperation.Builder();
									cacheEvictBuilder.setBeforeInvocation(cacheEvictOperation.isBeforeInvocation());
									cacheEvictBuilder.setCacheWide(cacheEvictOperation.isCacheWide());
									builder = cacheEvictBuilder;
								}
								else {
									CachePutOperation cachePutOperation = (CachePutOperation) operation;
									CachePutOperation.Builder cachePutBuilder = new CachePutOperation.Builder();
									String unless = cachePutOperation.getUnless();
									if (unless != null) {
										cachePutBuilder.setUnless(unless);
									}
									builder = cachePutBuilder;
								}
								builder.setName(operation.getName());
								builder.setCacheManager(operation.getCacheManager());
								builder.setCacheNames(this.cacheNames);
								builder.setCacheResolver(operation.getCacheResolver());
								builder.setCondition(operation.getCondition());
								builder.setKey(operation.getKey());
								builder.setKeyGenerator(operation.getKeyGenerator());
								return builder.build();
							})
							.toList();
		}
		else {
			cacheOperationsToUse = this.cacheOperations;
		}

		BeanFactory beanFactory = getBeanFactory();
		this.delegate.setBeanFactory(beanFactory);
		EvaluationContext evaluationContext = ExpressionUtils.createStandardEvaluationContext(beanFactory);
		this.delegate.setKeyGenerator((target, method, params) ->
				this.keyExpression.getValue(evaluationContext, params[0])); // NOSONAR
		this.delegate.setCacheOperationSources((method, targetClass) -> cacheOperationsToUse);
		this.delegate.afterPropertiesSet();

	}

	@Nullable
	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		CacheOperationInvoker operationInvoker =
				() -> {
					Object result = callback.execute();
					// Drop MessageBuilder optimization in favor of Serializable support in cache implementation.
					if (result instanceof AbstractIntegrationMessageBuilder<?>) {
						return ((AbstractIntegrationMessageBuilder<?>) result).build();
					}
					else {
						return result;
					}
				};

		return this.delegate.invoke(operationInvoker, target, message);
	}

	private static class IntegrationCacheAspect extends CacheAspectSupport {

		IntegrationCacheAspect() {
		}

		@Nullable
		Object invoke(CacheOperationInvoker invoker, Object target, Message<?> message) {
			return super.execute(invoker, target, HANDLE_REQUEST_METHOD, new Object[] {message}); // NOSONAR
		}

	}

}
