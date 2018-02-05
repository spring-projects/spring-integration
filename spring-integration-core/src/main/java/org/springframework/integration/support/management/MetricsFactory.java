/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.integration.support.management;

/**
 * Factories implementing this interface provide metric objects for message channels and
 * message handlers.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public interface MetricsFactory {

	/**
	 * Factory method to create an {@link AbstractMessageChannelMetrics}.
	 * @param name the name.
	 * @return the metrics.
	 */
	AbstractMessageChannelMetrics createChannelMetrics(String name);

	/**
	 * Factory method to create an {@link AbstractMessageChannelMetrics} for
	 * a pollable channel.
	 * @param name the name.
	 * @return the metrics.
	 * @since 5.0.2
	 */
	default AbstractMessageChannelMetrics createPollableChannelMetrics(String name) {
		return createChannelMetrics(name);
	}

	/**
	 * Factory method to create an {@link AbstractMessageHandlerMetrics}.
	 * @param name the name.
	 * @return the metrics.
	 */
	AbstractMessageHandlerMetrics createHandlerMetrics(String name);

}
