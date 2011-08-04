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

import java.util.UUID;

import org.springframework.integration.Message;
import org.springframework.integration.store.MessageStore;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Region;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class GemfireMessageStore implements MessageStore {

	private final Region<UUID, Message<?>> region;

	public GemfireMessageStore(Region<UUID, Message<?>> region) {
		Assert.notNull(region, "region must not be null");
		this.region = region;
	}

	public Message<?> getMessage(UUID id) {
		return this.region.get(id);
	}

	public <T> Message<T> addMessage(Message<T> message) {
		this.region.put(message.getHeaders().getId(), message);
		return message;
	}

	public Message<?> removeMessage(UUID id) {
		return this.region.remove(id);
	}

	public int getMessageCount() {
		return this.region.size();
	}

}
