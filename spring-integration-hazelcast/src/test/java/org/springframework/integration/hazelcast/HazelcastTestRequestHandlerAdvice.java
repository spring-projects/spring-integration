/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.integration.hazelcast;

import java.util.concurrent.CountDownLatch;

import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.Message;

/**
 * {@link AbstractRequestHandlerAdvice} advice class for Hazelcast Integration Unit Tests.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class HazelcastTestRequestHandlerAdvice extends AbstractRequestHandlerAdvice {

	public CountDownLatch executeLatch = null;

	public HazelcastTestRequestHandlerAdvice(int count) {
		this.executeLatch = new CountDownLatch(count);
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		try {
			return callback.execute();
		}
		finally {
			this.executeLatch.countDown();
		}
	}

}
