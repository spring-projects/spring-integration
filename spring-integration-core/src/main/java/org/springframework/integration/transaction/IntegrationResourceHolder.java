/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
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
public class IntegrationResourceHolder implements ResourceHolder {

	public static final String MESSAGE_SOURCE = "messageSource";

	public static final String INPUT_CHANNEL = "inputChannel";

	private volatile Message<?> message;

	private final Map<String, Object> attributes = new HashMap<String, Object>();

	public void setMessage(Message<?> message) {
		this.message = message;
	}

	public Message<?> getMessage() {
		return message;
	}

	/**
	 * Adds attribute to this {@link ResourceHolder} instance
	 *
	 * @param key The key.
	 * @param value The value.
	 */
	public void addAttribute(String key, Object value){
		this.attributes.put(key, value);
	}

	/**
	 * Will return an immutable Map of current attributes.
	 * If you need to add an attribute, use the {@link #addAttribute(String, Object)} method.
	 *
	 * @return the immutable map.
	 */
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@Override
	public void reset() {
	}

	@Override
	public void unbound() {
	}

	@Override
	public boolean isVoid() {
		return false;
	}

}
