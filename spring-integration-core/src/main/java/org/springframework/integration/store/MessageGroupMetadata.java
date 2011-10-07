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

package org.springframework.integration.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.integration.Message;
import org.springframework.util.Assert;

/**
 * Immutable Value Object holding metadata about a MessageGroup.
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class MessageGroupMetadata implements Serializable{

	private static final long serialVersionUID = 1L;


	private final Object groupId;

	private final List<UUID> markedMessageIds;

	private final List<UUID> unmarkedMessageIds;

	private final boolean complete;

	private final long timestamp;

	private final int lastReleasedMessageSequenceNumber;


	public MessageGroupMetadata(MessageGroup messageGroup) {
		Assert.notNull(messageGroup, "'messageGroup' must not be null");
		this.groupId = messageGroup.getGroupId();
		this.markedMessageIds = new ArrayList<UUID>();
		for (Message<?> message : messageGroup.getMarked()) {
			this.markedMessageIds.add(message.getHeaders().getId());
		}
		this.unmarkedMessageIds = new ArrayList<UUID>();
		for (Message<?> message : messageGroup.getUnmarked()) {
			this.unmarkedMessageIds.add(message.getHeaders().getId());
		}
		this.complete = messageGroup.isComplete();
		this.timestamp = messageGroup.getTimestamp();
		this.lastReleasedMessageSequenceNumber = messageGroup.getLastReleasedMessageSequenceNumber();
	}


	public Object getGroupId() {
		return this.groupId;
	}

	public List<UUID> getMarkedMessageIds() {
		return Collections.unmodifiableList(markedMessageIds);
	}

	public List<UUID> getUnmarkedMessageIds() {
		return Collections.unmodifiableList(this.unmarkedMessageIds);
	}

	public boolean isComplete() {
		return this.complete;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public int getLastReleasedMessageSequenceNumber() {
		return this.lastReleasedMessageSequenceNumber;
	}

}
