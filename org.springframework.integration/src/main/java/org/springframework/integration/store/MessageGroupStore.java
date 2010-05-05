/*
 * Copyright 2002-2008 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.store;

import java.util.Collection;

import org.springframework.integration.core.Message;

/**
 * Interface for storage operations on groups of messages linked by a correlation key.
 * 
 * @author Dave Syer
 * 
 * @since 2.0
 * 
 */
public interface MessageGroupStore {

	/**
	 * Return all Messages currently in the MessageStore that were stored using
	 * {@link #addMessageToGroup(Object, Collection)} with this correlation id.
	 * 
	 * @return a group of messages, empty if none exists for this key
	 */
	MessageGroup getMessageGroup(Object correlationKey);

	/**
	 * Store a message with an association to a correlation key. This can be used to group messages together instead of
	 * storing them just under their id.
	 * 
	 * @param correlationKey the correlation id to store the message under
	 * @param message a message
	 */
	void addMessageToGroup(Object correlationKey, Message<?> message);

	/**
	 * Persist the mark on all the messages from the group. The group is modified in the process as all its unmarked
	 * messages become marked.
	 * 
	 * @param group a MessageGroup with no unmarked messages
	 */
	void markMessageGroup(MessageGroup group);

	/**
	 * Remove the message group with this correlation key.
	 * 
	 * @param correlationKey the correlation id to remove
	 */
	void removeMessageGroup(Object correlationKey);

	/**
	 * Register a callback for when a message group is expired through {@link #expireMessageGroups(long)}.
	 * 
	 * @param callback a callback to execute when a message group is cleaned up
	 */
	void registerExpiryCallback(MessageGroupCallback callback);

	/**
	 * Extract all expired groups (those whose timestamp is less than the threshold provided) and call each of the
	 * registered callbacks on them in turn.
	 * 
	 * @param timestamp the timestamp threshold to use
	 * @return the number of message groups expired
	 * 
	 * @see #registerExpiryCallback(MessageGroupCallback)
	 */
	int expireMessageGroups(long timestamp);

}