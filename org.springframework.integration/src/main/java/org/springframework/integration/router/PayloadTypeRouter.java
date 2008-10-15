/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.router;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.util.Assert;

/**
 * A Message Router that resolves the {@link MessageChannel} based on the
 * {@link Message Message's} payload type.
 * 
 * @author Mark Fisher
 */
public class PayloadTypeRouter extends AbstractSingleChannelRouter {

	private volatile Map<Class<?>, MessageChannel> payloadTypeChannelMap =
			new ConcurrentHashMap<Class<?>, MessageChannel>();


	public void setPayloadTypeChannelMap(Map<Class<?>, MessageChannel> payloadTypeChannelMap) {
		Assert.notNull(payloadTypeChannelMap, "payloadTypeChannelMap must not be null");
		this.payloadTypeChannelMap = payloadTypeChannelMap;
	}

	@Override
	protected MessageChannel determineTargetChannel(Message<?> message) {
		return this.payloadTypeChannelMap.get(message.getPayload().getClass());
	}

}
