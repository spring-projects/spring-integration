/*
 * Copyright 2018-2019 the original author or authors.
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

import org.springframework.integration.acks.AcknowledgmentCallback.Status;
import org.springframework.lang.Nullable;

/**
 * Utility methods for acting on {@link AcknowledgmentCallback} headers.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0.1
 *
 */
public final class AckUtils {

	private AckUtils() {
	}

	/**
	 * ACCEPT an {@link AcknowledgmentCallback} if it's not null, supports auto ack
	 * and is not already ack'd.
	 * @param ackCallback the callback.
	 */
	public static void autoAck(@Nullable AcknowledgmentCallback ackCallback) {
		if (ackCallback != null && ackCallback.isAutoAck() && !ackCallback.isAcknowledged()) {
			ackCallback.acknowledge(Status.ACCEPT);
		}
	}

	/**
	 * REJECT an {@link AcknowledgmentCallback} if it's not null, supports auto ack
	 * and is not already ack'd.
	 * @param ackCallback the callback.
	 */
	public static void autoNack(@Nullable AcknowledgmentCallback ackCallback) {
		if (ackCallback != null && ackCallback.isAutoAck() && !ackCallback.isAcknowledged()) {
			ackCallback.acknowledge(Status.REJECT);
		}
	}

	/**
	 * ACCEPT the associated message if the callback is not null.
	 * @param ackCallback the callback.
	 */
	public static void accept(@Nullable AcknowledgmentCallback ackCallback) {
		if (ackCallback != null) {
			ackCallback.acknowledge(Status.ACCEPT);
		}
	}

	/**
	 * REJECT the associated message if the callback is not null.
	 * @param ackCallback the callback.
	 */
	public static void reject(@Nullable AcknowledgmentCallback ackCallback) {
		if (ackCallback != null) {
			ackCallback.acknowledge(Status.REJECT);
		}
	}

	/**
	 * REQUEUE the associated message if the callback is not null.
	 * @param ackCallback the callback.
	 */
	public static void requeue(@Nullable AcknowledgmentCallback ackCallback) {
		if (ackCallback != null) {
			ackCallback.acknowledge(Status.REQUEUE);
		}
	}

}
