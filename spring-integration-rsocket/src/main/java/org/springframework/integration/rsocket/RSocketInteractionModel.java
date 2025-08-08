/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.rsocket;

import io.rsocket.frame.FrameType;

/**
 * The RSocket protocol interaction models.
 *
 * @author Artem Bilan
 *
 * @since 5.2.2
 *
 * @see <a href="https://rsocket.io/">RSocket protocol official site</a>
 * @see FrameType
 */
public enum RSocketInteractionModel {

	/**
	 * The model for {@link io.rsocket.RSocket#fireAndForget} operation.
	 */
	fireAndForget(FrameType.REQUEST_FNF),

	/**
	 * The model for {@link io.rsocket.RSocket#requestResponse} operation.
	 */
	requestResponse(FrameType.REQUEST_RESPONSE),

	/**
	 * The model for {@link io.rsocket.RSocket#requestStream} operation.
	 */
	requestStream(FrameType.REQUEST_STREAM),

	/**
	 * The model for {@link io.rsocket.RSocket#requestChannel} operation.
	 */
	requestChannel(FrameType.REQUEST_CHANNEL);

	private final FrameType frameType;

	RSocketInteractionModel(FrameType frameType) {
		this.frameType = frameType;
	}

	public FrameType getFrameType() {
		return this.frameType;
	}

}
