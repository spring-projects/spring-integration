/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.gemfire.store;

import org.springframework.integration.Message;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides an implementation of {@link org.springframework.integration.store.MessageGroupStore} that delegates to a backend Gemfire instance.
 * Gemfire holds keys and values. This class provides a strategy to hold objects.
 *
 * @author Josh Long
 * @since 2.1
 */
public class KeyValueMessageGroupStore extends AbstractMessageGroupStore {

	/**
	 * Required {@link com.gemstone.gemfire.cache.Region} to managed the association of groups => {@link KeyValueMessageGroup}
	 */
	protected Map<Object, KeyValueMessageGroup> groupIdToMessageGroup;

	/**
	 * Required {@link com.gemstone.gemfire.cache.Region} to manage the #unmarked data
	 */
	protected Map<String, Message<?>> unmarked;

	/**
	 * Required {@link com.gemstone.gemfire.cache.Region} to manage the #marked data
	 */
	protected Map<String, Message<?>> marked;


	/**
	 * Create a KeyValueMessageGroupStore with two backing regions to handle the state management.
	 *
	 * @param groupIdToMessageGroup the region to associate
	 * @param marked				the collection that will hold which messages are marked (delivered)
	 * @param unmarked			  the collection that holds which messages are unmarked (not yet delivered)
	 */
	public KeyValueMessageGroupStore(Map<Object, KeyValueMessageGroup> groupIdToMessageGroup, Map<String, Message<?>> marked, Map<String, Message<?>> unmarked) {
		this.marked = marked;
		this.unmarked = unmarked;
		this.groupIdToMessageGroup = groupIdToMessageGroup;
	}


	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		return this.getMessageGroupInternal(groupId);
	}

	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		KeyValueMessageGroup group = getMessageGroupInternal(groupId);
		group.add(message);
		return group;
	}

	public MessageGroup markMessageGroup(MessageGroup group) {
		Object groupId = group.getGroupId();
		KeyValueMessageGroup internal = getMessageGroupInternal(groupId);
		internal.markAll();
		return internal;
	}

	public void removeMessageGroup(Object groupId) {
		groupIdToMessageGroup.remove(groupId);
	}

	public MessageGroup removeMessageFromGroup(Object key, Message<?> messageToRemove) {
		KeyValueMessageGroup group = getMessageGroupInternal(key);
		group.remove(messageToRemove);
		return group;
	}

	public MessageGroup markMessageFromGroup(Object key, Message<?> messageToMark) {
		KeyValueMessageGroup group = getMessageGroupInternal(key);
		group.mark(messageToMark);
		return group;
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		return new HashSet<MessageGroup>(groupIdToMessageGroup.values()).iterator();
	}

	protected KeyValueMessageGroup ensureMessageGroupHasReferencesToRegions(KeyValueMessageGroup keyValueMessageGroup) {
		if (keyValueMessageGroup == null) {
			return null;
		}
		keyValueMessageGroup.setMarked(this.marked);
		keyValueMessageGroup.setUnmarked(this.unmarked);
		return keyValueMessageGroup;
	}

	protected KeyValueMessageGroup getMessageGroupInternal(Object groupId) {
		if (!groupIdToMessageGroup.containsKey(groupId)) {
			groupIdToMessageGroup.put(groupId, new KeyValueMessageGroup(groupId));
		}
		return ensureMessageGroupHasReferencesToRegions(groupIdToMessageGroup.get( groupId));
	}

}