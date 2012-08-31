/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.transaction;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageSource;
import org.springframework.transaction.support.ResourceHolder;

/**
 * An implementation of the {@link ResourceHolder} which holds an instance of the current Message
 * and the synchronization resource
 *
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 2.2
 *
 */
public class MessageSourceResourceHolder implements ResourceHolder {

	private final MessageSource<?> source;

	private volatile Message<?> message;

	private final Map<String, Object> attributes = new HashMap<String, Object>();

	public MessageSourceResourceHolder(MessageSource<?> source) {
		this.source = source;
	}

	protected MessageSource<?> getMessageSource() {
		return this.source;
	}

	public void setMessage(Message<?> message) {
		this.message = message;
	}

	public Message<?> getMessage() {
		return message;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void reset() {
	}

	public void unbound() {
	}

	public boolean isVoid() {
		return false;
	}

}