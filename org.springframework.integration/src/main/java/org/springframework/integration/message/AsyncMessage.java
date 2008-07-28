/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.message;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.integration.message.MessagingException;

/**
 * A Message wrapper for asynchronous operations. Implements both
 * Message and Future so that simple blocking invocations on the
 * Message (e.g. {@link #getPayload()}) are still allowed, while
 * timeout-aware methods such as {@link #get(long, TimeUnit)}
 * are also supported.
 * 
 * @author Mark Fisher
 */
public class AsyncMessage<T> implements Future<Message<T>>, Message<T> {

	private final Future<Message<T>> future;


	public AsyncMessage(Future<Message<T>> future) {
		this.future = future;
	}


	public boolean cancel(boolean mayInterruptIfRunning) {
		return this.future.cancel(mayInterruptIfRunning);
	}

	public Message<T> get() throws InterruptedException, ExecutionException {
		return this.future.get();
	}

	public Message<T> get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return this.future.get(timeout, unit);
	}

	public boolean isCancelled() {
		return this.future.isCancelled();
	}

	public boolean isDone() {
		return this.future.isDone();
	}

	public MessageHeaders getHeaders() {
		try {
			return this.future.get().getHeaders();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (ExecutionException e) {
			throw new MessagingException("failure occurred in AsyncMessage", e);
		}
	}

	public Object getId() {
		try {
			return this.future.get().getId();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (ExecutionException e) {
			throw new MessagingException("failure occurred in AsyncMessage", e);
		}
	}

	public T getPayload() {
		try {
			return this.future.get().getPayload();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (ExecutionException e) {
			throw new MessagingException("failure occurred in AsyncMessage", e);
		}
	}

}
