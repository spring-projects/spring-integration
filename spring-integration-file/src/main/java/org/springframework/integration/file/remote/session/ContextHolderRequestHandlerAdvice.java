/*
 * Copyright 2015-2023 the original author or authors.
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

package org.springframework.integration.file.remote.session;

import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Provides a key for the context holder.
 * This key could for example be stored in a message header.
 * @author Adel Haidar
 * @since 6.1
 */
public class ContextHolderRequestHandlerAdvice extends AbstractRequestHandlerAdvice {

	public final Function<Message<?>, Object> keyProvider;

	public final Consumer<Object> contextSetHook;

	public final Consumer<Object> contextClearHook;

	/**
	 * Constructor for the ContextHolderRequestHandlerAdvice.
	 * @param keyProvider The key provider function.
	 * @param contextSetHook The context set hook function.
	 * @param contextClearHook The context clear hook function.
	 */
	public ContextHolderRequestHandlerAdvice(Function<Message<?>, Object> keyProvider, Consumer<Object> contextSetHook, Consumer<Object> contextClearHook) {
		Assert.notNull(keyProvider, "'keyProvider' must not be null");
		Assert.notNull(contextSetHook, "'contextSetHook' must not be null");
		Assert.notNull(contextClearHook, "'contextClearHook' must not be null");
		this.keyProvider = keyProvider;
		this.contextSetHook = contextSetHook;
		this.contextClearHook = contextClearHook;
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		final Object key = this.keyProvider.apply(message);
		logger.trace("Setting context key to: " + key + " message: " + message);
		try {
			this.contextSetHook.accept(key);
			return callback.execute();
		}
		finally {
			this.contextClearHook.accept(key);
		}
	}

}
