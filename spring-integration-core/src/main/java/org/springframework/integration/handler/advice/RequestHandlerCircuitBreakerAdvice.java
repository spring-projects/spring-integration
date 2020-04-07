/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * A circuit breaker that stops calling a failing service after threshold
 * failures, until halfOpenAfter milliseconds has elapsed. A successful
 * call resets the failure counter.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class RequestHandlerCircuitBreakerAdvice extends AbstractRequestHandlerAdvice {

	/**
	 * A default failures threshold as {@value DEFAULT_THRESHOLD}.
	 */
	public static final int DEFAULT_THRESHOLD = 5;

	/**
	 * A half-open duration as {@value DEFAULT_HALF_OPEN_AFTER} .
	 */
	public static final int DEFAULT_HALF_OPEN_AFTER = 1000;

	private int threshold = DEFAULT_THRESHOLD;

	private long halfOpenAfter = DEFAULT_HALF_OPEN_AFTER;

	private final ConcurrentMap<Object, AdvisedMetadata> metadataMap = new ConcurrentHashMap<>();

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

	public void setHalfOpenAfter(long halfOpenAfter) {
		this.halfOpenAfter = halfOpenAfter;
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		AdvisedMetadata metadata = this.metadataMap.get(target);
		if (metadata == null) {
			this.metadataMap.putIfAbsent(target, new AdvisedMetadata());
			metadata = this.metadataMap.get(target);
		}
		if (metadata.getFailures().get() >= this.threshold &&
				System.currentTimeMillis() - metadata.getLastFailure() < this.halfOpenAfter) {
			throw new CircuitBreakerOpenException(message, "Circuit Breaker is Open for " + target);
		}
		try {
			Object result = callback.execute();
			if (logger.isDebugEnabled() && metadata.getFailures().get() > 0) {
				logger.debug("Closing Circuit Breaker for " + target);
			}
			metadata.getFailures().set(0);
			return result;
		}
		catch (Exception e) {
			metadata.getFailures().incrementAndGet();
			metadata.setLastFailure(System.currentTimeMillis());
			if (e instanceof ThrowableHolderException) { // NOSONAR
				throw (ThrowableHolderException) e;
			}
			else {
				throw new ThrowableHolderException(e);
			}
		}
	}

	private static class AdvisedMetadata {

		private final AtomicInteger failures = new AtomicInteger();

		private volatile long lastFailure;

		AdvisedMetadata() {
		}

		private long getLastFailure() {
			return this.lastFailure;
		}

		private void setLastFailure(long lastFailure) {
			this.lastFailure = lastFailure;
		}

		private AtomicInteger getFailures() {
			return this.failures;
		}

	}

	/**
	 * An exception thrown when the circuit breaker is in an open state.
	 */
	public static final class CircuitBreakerOpenException extends MessagingException {

		private static final long serialVersionUID = 1L;

		public CircuitBreakerOpenException(Message<?> message, String description) {
			super(message, description);
		}

	}

}
