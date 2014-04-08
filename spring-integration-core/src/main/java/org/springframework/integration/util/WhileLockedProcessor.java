/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.util;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.MessagingException;

/**
 * A simple strategy callback class that allows you to provide
 * a code that needs to be executed under {@link Lock} provided by
 * {@link LockRegistry}
 * A typical usage would be to provide implementation of {@link #whileLocked()} method and
 * then call {@link #doWhileLocked()}
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 *
 */
public abstract class WhileLockedProcessor {
	private final Object key;
	private final LockRegistry lockRegistry;

	public WhileLockedProcessor(LockRegistry lockRegistry,  Object key){
		this.key = key;
		this.lockRegistry = lockRegistry;
	}
	public final void doWhileLocked() throws IOException{
		Lock lock = lockRegistry.obtain(key);
		try {
			lock.lockInterruptibly();
			try {
				this.whileLocked();
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessagingException("Thread was interrupted while performing task", e);
		}
	}

	/**
	 * Override this method to provide the behavior that needs to be executed
	 * while under the lock.
	 *
	 * @throws IOException Any IOException.
	 */
	protected abstract void whileLocked() throws IOException;
}
