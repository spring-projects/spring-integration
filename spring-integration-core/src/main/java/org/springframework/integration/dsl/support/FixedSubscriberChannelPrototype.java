/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.dsl.support;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * An "artificial" {@link MessageChannel} implementation which will be unwrapped to the
 * {@link org.springframework.integration.channel.FixedSubscriberChannel} on the bean
 * registration phase.
 * For internal use only.
 *
 * @author Artem Bilan
 * @since 5.0
 *
 * @see org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor
 */
public class FixedSubscriberChannelPrototype implements MessageChannel {

	private final String name;

	public FixedSubscriberChannelPrototype() {
		this(null);
	}

	public FixedSubscriberChannelPrototype(@Nullable String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean send(Message<?> message) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "FixedSubscriberChannelPrototype{" +
				"name='" + this.name + '\'' +
				'}';
	}

}
