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

package org.springframework.integration.channel;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.springframework.integration.message.Message;

/**
 * Simple implementation of a message channel. Each {@link Message} is
 * placed in a queue whose capacity may be specified upon construction. If no
 * capacity is specified, the {@link #DEFAULT_CAPACITY} will be used.
 * 
 * @author Mark Fisher
 */
public class SimpleChannel extends BaseBlockingQueueChannel {

	/**
	 * Create a channel with the specified queue capacity and dispatcher policy.
	 */
	public SimpleChannel(int capacity, DispatcherPolicy dispatcherPolicy) {
		super((capacity > 0) ? new LinkedBlockingQueue<Message<?>>(capacity) :
				new SynchronousQueue<Message<?>>(true), dispatcherPolicy);
	}

	/**
	 * Create a channel with the specified queue capacity.
	 */
	public SimpleChannel(int capacity) {
		this(capacity, null);
	}

	/**
	 * Create a channel with the default queue capacity.
	 */
	public SimpleChannel() {
		this(DEFAULT_CAPACITY, null);
	}

}
