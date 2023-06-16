/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.jpa.test;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;

/**
 *
 * @author Gunnar Hillert
 * @author Christian Tzolov
 *
 * @since 2.2
 *
 */
public final class Consumer {

	private static final Log logger = LogFactory.getLog(Consumer.class);

	private static final BlockingQueue<Message<Collection<?>>> MESSAGES = new LinkedBlockingQueue<Message<Collection<?>>>();

	private final Lock lock = new ReentrantLock();

	public void receive(Message<Collection<?>> message) {
		this.lock.lock();
		try {
			logger.info("Service Activator received Message: " + message);
			MESSAGES.add(message);
		}
		finally {
			this.lock.unlock();
		}
	}

	public Message<Collection<?>> poll(long timeoutInMillis) throws InterruptedException {
		this.lock.lock();
		try {
			return MESSAGES.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
		}
		finally {
			this.lock.unlock();
		}
	}

}
