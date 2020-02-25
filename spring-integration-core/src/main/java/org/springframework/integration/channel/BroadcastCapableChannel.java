/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.channel;

import org.springframework.messaging.SubscribableChannel;

/**
 * A {@link SubscribableChannel} variant for implementations with broadcasting capabilities.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public interface BroadcastCapableChannel extends SubscribableChannel {

	/**
	 * Return a state of this channel in regards of broadcasting capabilities.
	 * @return the state of this channel in regards of broadcasting capabilities.
	 */
	default boolean isBroadcast() {
		return true;
	}

}
