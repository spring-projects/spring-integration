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

package org.springframework.integration.kafka.dsl;

import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.kafka.channel.AbstractKafkaChannel;

/**
 *
 * Spec for a message channel backed by an Apache Kafka topic.
 *
 * @param <S> the spec type.
 * @param <C> the channel type.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public abstract class AbstractKafkaChannelSpec<S extends AbstractKafkaChannelSpec<S, C>, C extends AbstractKafkaChannel>
		extends MessageChannelSpec<S, C> {

	protected String groupId; // NOSONAR

	@Override
	public S id(String idToSet) { // NOSONAR - increase visibility
		return super.id(idToSet);
	}

	/**
	 * Set the group id to use on the consumer side.
	 * @param group the group id.
	 * @return the spec.
	 */
	public S groupId(String group) {
		this.groupId = group;
		return _this();
	}

}
