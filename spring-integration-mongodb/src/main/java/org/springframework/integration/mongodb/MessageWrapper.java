/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.mongodb;

import java.util.Map;

import org.springframework.data.annotation.Transient;
import org.springframework.integration.Message;
import org.springframework.util.Assert;

/**
 * Wrapper class used for storing Messages in MongoDB along with their "group" metadata.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 *
 */
public class MessageWrapper {

	private volatile Object _groupId;

	@Transient
	private final Message<?> message;

	private final Object payload;

	@SuppressWarnings("unused")
	private final Map<String, ?> headers;

	@SuppressWarnings("unused")
	private final String _payloadType;

	private volatile long _group_timestamp;

	private volatile long _group_update_timestamp;

	private volatile int _last_released_sequence;

	private volatile boolean _group_complete;

	public MessageWrapper(Message<?> message) {
		Assert.notNull(message, "'message' must not be null");
		this.message = message;
		this.payload = message.getPayload();
		this.headers = message.getHeaders();
		this._payloadType = this.payload.getClass().getName();
	}

	public int get_LastReleasedSequenceNumber() {
		return _last_released_sequence;
	}

	public long get_Group_timestamp() {
		return _group_timestamp;
	}

	public boolean get_Group_complete() {
		return _group_complete;
	}

	public Object get_GroupId() {
		return _groupId;
	}

	public Message<?> getMessage() {
		return message;
	}

	public void set_GroupId(Object groupId) {
		this._groupId = groupId;
	}

	public void set_Group_timestamp(long groupTimestamp) {
		this._group_timestamp = groupTimestamp;
	}

	public long get_Group_update_timestamp() {
		return _group_update_timestamp;
	}

	public void set_Group_update_timestamp(long lastModified) {
		this._group_update_timestamp = lastModified;
	}

	public void set_LastReleasedSequenceNumber(int lastReleasedSequenceNumber) {
		this._last_released_sequence = lastReleasedSequenceNumber;
	}

	public void set_Group_complete(boolean completedGroup) {
		this._group_complete = completedGroup;
	}
}