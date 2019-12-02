/*
 * Copyright 2019 the original author or authors.
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
