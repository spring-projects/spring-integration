/*
 * Copyright 2015-2022 the original author or authors.
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

/**
 * Provides a key for the context holder.This key could for example be stored in a message header.
 *
 * @author Adel Haidar
 * @since 6.1
 */
public class ContextHolderRequestHandlerAdvice extends AbstractRequestHandlerAdvice {
	public Function<Message<?>, Object> keyProvider;

	public Consumer<Object> contextSetHook;

	public Consumer<Object> contextClearHook;

	/**
	 * Returns the key provider function.
	 * @return The key provider function.
	 *
	 */
	public Function<Message<?>, Object> getKeyProvider() {
		return this.keyProvider;
	}

	/**
	 * Sets the key provider function.
	 * @param keyProvider the key provider.
	 */
	public void setKeyProvider(Function<Message<?>, Object> keyProvider) {
		this.keyProvider = keyProvider;
	}

	/**
	 * Returns the context set hook function.
	 * @return The context set hook function.
	 *
	 */
	public Consumer<Object> getContextSetHook() {
		return this.contextSetHook;
	}

	/**
	 * Sets the context set hook function.
	 * @param contextSetHook the context set hook function.
	 */
	public void setContextSetHook(Consumer<Object> contextSetHook) {
		this.contextSetHook = contextSetHook;
	}

	/**
	 * Returns the context clear hook function.
	 * @return The context clear hook function.
	 */
	public Consumer<Object> getContextClearHook() {
		return this.contextClearHook;
	}

	/**
	 * Sets the context clear hook function.
	 * @param contextClearHook the context clear hook function.
	 */
	public void setContextClearHook(Consumer<Object> contextClearHook) {
		this.contextClearHook = contextClearHook;
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		Object result;
		final Object key = this.keyProvider.apply(message);
		try {
			result = callback.execute();
			setContext(key);
		}
		catch (Exception e) {
			result = e;
			logger.error("Failure setting context holder for " + message + ": " + e.getMessage());
		}
		finally {
			contextClearHook(key);
		}
		return result;
	}

	private void setContext(Object key) {
		this.contextSetHook.accept(key);
	}

	private void contextClearHook(Object key) {
		this.contextClearHook.accept(key);
	}

}
