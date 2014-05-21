/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.Date;

import org.springframework.integration.event.IntegrationEvent;
import org.springframework.integration.support.context.NamedComponent;

/**
 * Event representing the expiration of a message group.
 *
 * @author Gary Russell
 * @since 4.0.1
 */
public class MessageGroupExpiredEvent extends IntegrationEvent {

	private static final long serialVersionUID = -7126221042599333919L;

	private final Object groupId;

	private final int messageCount;

	private final Date lastModified;

	private final Date expired;

	private final boolean discarded;

	public MessageGroupExpiredEvent(Object source, Object groupId, int messageCount, Date lastModified, Date expired,
			boolean discarded) {
		super(source);
		this.groupId = groupId;
		this.messageCount = messageCount;
		this.lastModified = lastModified;
		this.expired = expired;
		this.discarded = discarded;
	}

	public Object getGroupId() {
		return groupId;
	}

	public int getMessageCount() {
		return messageCount;
	}

	protected Date getLastModified() {
		return lastModified;
	}

	public Date getExpired() {
		return expired;
	}

	public boolean isDiscarded() {
		return discarded;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Object sourceName;
		if (this.source instanceof NamedComponent) {
			sourceName = ((NamedComponent) source).getComponentName();
		}
		else {
			sourceName = this.source.toString();
		}
		builder.append("MessageGroupExpiredEvent [groupId=")
			.append(this.groupId)
			.append(", messageCount=")
			.append(this.messageCount)
			.append(", lastModified=")
			.append(this.lastModified)
			.append(", expiredAt=")
			.append(this.expired)
			.append(", discarded=")
			.append(this.discarded)
			.append(", source=")
			.append(sourceName)
			.append("]");
		return builder.toString();
	}

}
