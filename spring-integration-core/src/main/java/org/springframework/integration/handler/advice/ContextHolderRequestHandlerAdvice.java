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

package org.springframework.integration.handler.advice;

import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An {@link AbstractRequestHandlerAdvice} implementation to store and reset
 * a value into/from some context (e.g. {@link ThreadLocal}) against a request message.
 * The context is populated before {@code callback.execute()} and reset after.
 *
 * @author Adel Haidar
 * @author Artem Bilan
 *
 * @since 6.1
 */
public class ContextHolderRequestHandlerAdvice extends AbstractRequestHandlerAdvice {

	public final Function<Message<?>, Object> valueProvider;

	public final Consumer<Object> contextSetHook;

	public final Runnable contextClearHook;

	/**
	 * Construct an instance based on the provided hooks.
	 * @param valueProvider The key provider function.
	 * @param contextSetHook The context set hook consumer.
	 * @param contextClearHook The context clear hook consumer.
	 */
	public ContextHolderRequestHandlerAdvice(Function<Message<?>, Object> valueProvider,
			Consumer<Object> contextSetHook, Runnable contextClearHook) {

		Assert.notNull(valueProvider, "'valueProvider' must not be null");
		Assert.notNull(contextSetHook, "'contextSetHook' must not be null");
		Assert.notNull(contextClearHook, "'contextClearHook' must not be null");
		this.valueProvider = valueProvider;
		this.contextSetHook = contextSetHook;
		this.contextClearHook = contextClearHook;
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		Object value = this.valueProvider.apply(message);
		logger.trace(() -> "Setting context value to: " + value + " from message: " + message);
		try {
			this.contextSetHook.accept(value);
			return callback.execute();
		}
		finally {
			this.contextClearHook.run();
		}
	}

}
