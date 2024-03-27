/*
 * Copyright 2014-2024 the original author or authors.
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

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;

/**
 * Defines a minimal message group store with basic capabilities.
 *
 * @author Gary Russell
 *
 * @since 4.0
 *
 */
public interface BasicMessageGroupStore {

	/**
	 * Return the size of this MessageGroup.
	 * @param groupId The group identifier.
	 * @return The size.
	 */
	@ManagedAttribute
	int messageGroupSize(Object groupId);

	/**
	 * Return all Messages currently in the MessageStore that were stored using
	 * {@link #addMessageToGroup(Object, Message)} with this group id.
	 * @param groupId The group identifier.
	 * @return A group of messages, empty if none exists for this key.
	 */
	MessageGroup getMessageGroup(Object groupId);

	/**
	 * Store a message with an association to a group id. This can be used to group messages together.
	 * @param groupId The group id to store the message under.
	 * @param message A message.
	 * @return The message group.
	 */
	MessageGroup addMessageToGroup(Object groupId, Message<?> message);

	/**
	 * Poll Message from this {@link MessageGroup} (in FIFO style if supported by the implementation)
	 * while also removing the polled {@link Message}.
	 * @param groupId The group identifier.
	 * @return The message.
	 */
	Message<?> pollMessageFromGroup(Object groupId);

	/**
	 * Remove the message group with this id.
	 * @param groupId The id of the group to remove.
	 */
	void removeMessageGroup(Object groupId);

}
