/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
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
