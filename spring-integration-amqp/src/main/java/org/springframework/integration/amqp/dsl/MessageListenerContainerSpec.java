/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.integration.dsl.IntegrationComponentSpec;

/**
 * Base class for container specs.
 *
 * @param <S> the current spec extension type
 * @param <C> the listener container type
 *
 * @author Gary Russell
 *
 * @since 6.0
 *
 */
public abstract class MessageListenerContainerSpec<S extends MessageListenerContainerSpec<S, C>,
		C extends MessageListenerContainer>
		extends IntegrationComponentSpec<S, C> {

	/**
	 * Set the queue names.
	 * @param queueNames the queue names.
	 * @return this spec.
	 */
	public S queueName(String... queueNames) {
		this.target.setQueueNames(queueNames);
		return _this();
	}

}
