/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.integration.store;

import java.util.Collection;

import org.springframework.messaging.Message;

/**
 * The {@link MessageGroup} factory strategy.
 * This strategy is used from the {@link MessageGroup}-aware components, e.g. {@code MessageGroupStore}.
 *
 * @author Artem Bilan
 * @since 4.3
 */
public interface MessageGroupFactory {

	/**
	 * Create a {@link MessageGroup} instance based on the provided {@code groupId}.
	 * @param groupId the group id to use.
	 * @return the {@link MessageGroup} instance.
	 */
	MessageGroup create(Object groupId);

	/**
	 * Create a {@link MessageGroup} instance based on the provided {@code groupId}
	 * and with the {@code messages} for the group.
	 * @param messages the messages for the group.
	 * @param groupId the group id to use.
	 * @return the {@link MessageGroup} instance.
	 */
	MessageGroup create(Collection<? extends Message<?>> messages, Object groupId);

	/**
	 * Create a {@link MessageGroup} instance based on the provided {@code groupId}
	 * and with the {@code messages} for the group.
	 * In addition the creating {@code timestamp} and {@code complete} flag may be used to customize
	 * the target {@link MessageGroup} object.
	 * @param messages the messages for the group.
	 * @param groupId the group id to use.
	 * @param timestamp the creation time.
	 * @param complete the {@code boolean} flag to indicate that group is completed.
	 * @return the {@link MessageGroup} instance.
	 */
	MessageGroup create(Collection<? extends Message<?>> messages, Object groupId, long timestamp,
			boolean complete);

	/**
	 * Create a {@link MessageGroup} instance based on the provided {@code groupId}.
	 * The {@link MessageGroupStore} may be consulted for the messages and metadata for the {@link MessageGroup}.
	 * @param messageGroupStore the {@link MessageGroupStore} for additional {@link MessageGroup} information.
	 * @param groupId the group id to use.
	 * @return the {@link MessageGroup} instance.
	 */
	MessageGroup create(MessageGroupStore messageGroupStore, Object groupId);

	/**
	 * Create a {@link MessageGroup} instance based on the provided {@code groupId}.
	 * The {@link MessageGroupStore} may be consulted for the messages and metadata for the {@link MessageGroup}.
	 * In addition the creating {@code timestamp} and {@code complete} flag may be used to customize
	 * the target {@link MessageGroup} object.
	 * @param messageGroupStore the {@link MessageGroupStore} for additional {@link MessageGroup} information.
	 * @param groupId the group id to use.
	 * @param timestamp the creation time.
	 * @param complete the {@code boolean} flag to indicate that group is completed.
	 * @return the {@link MessageGroup} instance.
	 */
	MessageGroup create(MessageGroupStore messageGroupStore, Object groupId, long timestamp,
			boolean complete);

}
