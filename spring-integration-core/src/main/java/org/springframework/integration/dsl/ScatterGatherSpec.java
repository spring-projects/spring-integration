/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl;

import org.springframework.integration.scattergather.ScatterGatherHandler;
import org.springframework.messaging.MessageChannel;

/**
 * A {@link GenericEndpointSpec} extension for the {@link ScatterGatherHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see ScatterGatherHandler
 */
public class ScatterGatherSpec extends ConsumerEndpointSpec<ScatterGatherSpec, ScatterGatherHandler> {

	ScatterGatherSpec(ScatterGatherHandler messageHandler) {
		super(messageHandler);
	}

	/**
	 * Specify a {@link MessageChannel} (optional) which is used internally
	 * in the {@link ScatterGatherHandler} for gathering (aggregate) results for scattered requests.
	 * @param gatherChannel the {@link MessageChannel} for gathering results.
	 * @return the current {@link ScatterGatherSpec} instance.
	 */
	public ScatterGatherSpec gatherChannel(MessageChannel gatherChannel) {
		this.handler.setGatherChannel(gatherChannel);
		return this;
	}

	/**
	 * Specify a timeout (in milliseconds) for the
	 * {@link org.springframework.messaging.PollableChannel#receive(long)} operation
	 * to wait for gathering results to output.
	 * Defaults to {@code -1} - to wait indefinitely.
	 * @param gatherTimeout the {@link org.springframework.messaging.PollableChannel} receive timeout.
	 * @return the current {@link ScatterGatherSpec} instance.
	 */
	public ScatterGatherSpec gatherTimeout(long gatherTimeout) {
		this.handler.setGatherTimeout(gatherTimeout);
		return this;
	}

}
