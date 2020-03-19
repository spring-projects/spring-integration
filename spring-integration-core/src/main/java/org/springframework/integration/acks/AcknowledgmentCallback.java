/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.integration.acks;

/**
 * A general abstraction over acknowledgments.
 *
 * @author Gary Russell
 *
 * @since 5.0.1
 *
 */
@FunctionalInterface
public interface AcknowledgmentCallback extends SimpleAcknowledgment {

	/**
	 * Acknowledge the message.
	 * @param status the status.
	 */
	void acknowledge(Status status);

	@Override
	default void acknowledge() {
		acknowledge(Status.ACCEPT);
	}

	/**
	 * Implementations must implement this to indicate when the ack has been
	 * processed by the user so that the framework can auto-ack if needed.
	 * @return true if the message is already acknowledged.
	 */
	default boolean isAcknowledged() {
		return false;
	}

	/**
	 * Disable auto acknowledgment by a {@code SourcePollingChannelAdapter}
	 * or {@code MessageSourcePollingTemplate}. Not all implementations support
	 * this - for example, the Kafka message source.
	 */
	default void noAutoAck() {
		throw new UnsupportedOperationException("You cannot disable auto acknowledgment with this implementation");
	}

	/**
	 * Return true if this acknowledgment supports auto ack when it has not been
	 * already ack'd by the application.
	 * @return true if auto ack is supported.
	 */
	default boolean isAutoAck() {
		return true;
	}

	enum Status {

		/**
		 * Mark the message as accepted.
		 */
		ACCEPT,

		/**
		 * Mark the message as rejected.
		 */
		REJECT,

		/**
		 * Reject the message and requeue so that it will be redelivered.
		 */
		REQUEUE

	}

}
