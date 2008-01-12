/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.bus;

import org.springframework.integration.channel.MessageChannel;

/**
 * A {@link MessageDispatcher} that polls a {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class ChannelPollingMessageDispatcher extends BasePollingMessageDispatcher {

	public ChannelPollingMessageDispatcher(MessageChannel channel, int period) {
		this(channel, ConsumerPolicy.newPollingPolicy(period));
	}

	public ChannelPollingMessageDispatcher(MessageChannel channel, ConsumerPolicy policy) {
		super(new ChannelPollingMessageRetriever(channel, policy));
	}

}
